package com.zjl.log.codeless

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import groovy.io.FileType
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

class LogTransform extends Transform {
    private LogTransformHelper transformHelper

    LogTransform(LogTransformHelper transformHelper) {
        this.transformHelper = transformHelper
    }

    @Override
    String getName() {
        return "CodelessLogTransform"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    /**
     * 打印提示信息
     */
    private void printCopyRight() {
        println()
        println("\033[40;32m" + "####################################################################" + "\033[0m")
        println("\033[40;32m" + "########                                                    ########" + "\033[0m")
        println("\033[40;32m" + "########                                                    ########" + "\033[0m")
        println("\033[40;32m" + "########                 CodelessLog 编译插件               ########" + "\033[0m")
        println("\033[40;32m" + "########                                                    ########" + "\033[0m")
        println("\033[40;32m" + "########                                                    ########" + "\033[0m")
        println("\033[40;32m" + "####################################################################" + "\033[0m")
        println()
    }

    @Override
    void transform(Context context, Collection<TransformInput> inputs, Collection<TransformInput> referencedInputs, TransformOutputProvider outputProvider, boolean isIncremental)
            throws IOException, TransformException, InterruptedException {
        /**
         * 打印提示信息
         */
        printCopyRight()

        if (!incremental) {
            outputProvider.deleteAll()
        }

        transformHelper.onTransform()

        /**
         * 遍历输入文件
         */
        inputs.each { TransformInput input ->
            /**
             * 遍历 jar
             */
            input.jarInputs.each { JarInput jarInput ->
                String destName = jarInput.file.name
                /**
                 * 截取文件路径的md5值重命名输出文件,因为可能同名,会覆盖
                 */
                def hexName = DigestUtils.md5Hex(jarInput.file.absolutePath).substring(0, 8)
                if (destName.endsWith(".jar")) {
                    destName = destName.substring(0, destName.length() - 4)
                }
                /** 获得输出文件*/
                File dest = outputProvider.getContentLocation(destName + "_" + hexName, jarInput.contentTypes, jarInput.scopes, Format.JAR)

                def modifiedJar = null
                if (!transformHelper.disableJar) {
                    Logger.info("开始遍历 jar：" + jarInput.file.absolutePath)
                    modifiedJar = modifyJarFile(jarInput.file, context.getTemporaryDir())
                    Logger.info("结束遍历 jar：" + jarInput.file.absolutePath)
                }
                if (modifiedJar == null) {
                    modifiedJar = jarInput.file
                }
                FileUtils.copyFile(modifiedJar, dest)
            }

            /**
             * 遍历目录
             */
            input.directoryInputs.each { DirectoryInput directoryInput ->
                File dest = outputProvider.getContentLocation(directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
                //Logger.info("||-->开始遍历特定目录  ${dest.absolutePath}")
                File dir = directoryInput.file
                if (dir) {
                    HashMap<String, File> modifyMap = new HashMap<>()
                    dir.traverse(type: FileType.FILES, nameFilter: ~/.*\.class/) {
                        File classFile ->
                            File modified = modifyClassFile(dir, classFile, context.getTemporaryDir())
                            if (modified != null) {
                                //key为相对路径
                                modifyMap.put(classFile.absolutePath.replace(dir.absolutePath, ""), modified)
                            }
                    }
                    FileUtils.copyDirectory(directoryInput.file, dest)
                    modifyMap.entrySet().each {
                        Map.Entry<String, File> en ->
                            File target = new File(dest.absolutePath + en.getKey())
                            if (target.exists()) {
                                target.delete()
                            }
                            FileUtils.copyFile(en.getValue(), target)
                            en.getValue().delete()
                    }
                }
            }
        }
    }

    /**
     * 修改 jar 文件中对应字节码
     */
    private File modifyJarFile(File jarFile, File tempDir) {
        if (jarFile) {
            return modifyJar(jarFile, tempDir, true)

        }
        return null
    }

    private File modifyJar(File jarFile, File tempDir, boolean nameHex) {
        /**
         * 读取原 jar
         */
        def file = new JarFile(jarFile)

        /**
         * 设置输出到的 jar
         */
        def hexName = ""
        if (nameHex) {
            hexName = DigestUtils.md5Hex(jarFile.absolutePath).substring(0, 8)
        }
        def outputJar = new File(tempDir, hexName + jarFile.name)
        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(outputJar))
        Enumeration enumeration = file.entries()

        while (enumeration.hasMoreElements()) {
            JarEntry jarEntry = (JarEntry) enumeration.nextElement()
            InputStream inputStream = file.getInputStream(jarEntry)

            String entryName = jarEntry.getName()
            if (entryName.endsWith(".DSA") || entryName.endsWith(".SF")) {
                //ignore
            } else {
                String className

                ZipEntry zipEntry = new ZipEntry(entryName)

                jarOutputStream.putNextEntry(zipEntry)

                byte[] modifiedClassBytes = null
                byte[] sourceClassBytes = null
                try {
                    sourceClassBytes = IOUtils.toByteArray(inputStream)
                } catch (Exception e) {
                    return null
                }
                if (entryName.endsWith(".class")) {
                    className = entryName.replace("/", ".").replace(".class", "")
                    ClassAnalysis classNameAnalytics = transformHelper.analyze(className)
                    if (classNameAnalytics.isShouldModify) {
                        modifiedClassBytes = modifyClasses(sourceClassBytes, classNameAnalytics)
                    }
                }
                if (modifiedClassBytes == null) {
                    jarOutputStream.write(sourceClassBytes)
                } else {
                    jarOutputStream.write(modifiedClassBytes)
                }
                jarOutputStream.closeEntry()
            }
        }
        jarOutputStream.close()
        file.close()
        return outputJar
    }

    private byte[] modifyClasses(byte[] srcByteCode, ClassAnalysis classNameAnalytics) {
        try {
            return modifyClass(srcByteCode, classNameAnalytics)
        } catch (UnsupportedOperationException e) {
            throw e
        } catch (Exception ex) {
            return srcByteCode
        }
    }
    /**
     * 真正修改类中方法字节码
     */
    private byte[] modifyClass(byte[] srcClass, ClassAnalysis classNameAnalytics) throws IOException {
        try {
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS)
            ClassVisitor classVisitor = new HookClassVisitor(classWriter, classNameAnalytics, transformHelper)
            ClassReader cr = new ClassReader(srcClass)
            cr.accept(classVisitor, ClassReader.EXPAND_FRAMES)
            return classWriter.toByteArray()
        } catch (UnsupportedOperationException e) {
            throw e
        } catch (Exception ex) {
            return srcClass
        }
    }

    /**
     * 目录文件中修改对应字节码
     */
    private File modifyClassFile(File dir, File classFile, File tempDir) {
        File modified = null
        FileOutputStream outputStream = null
        try {
            String className = path2ClassName(classFile.absolutePath.replace(dir.absolutePath + File.separator, ""))
            ClassAnalysis classNameAnalytics = transformHelper.analyze(className)
            if (classNameAnalytics.isShouldModify) {
                byte[] sourceClassBytes = IOUtils.toByteArray(new FileInputStream(classFile))
                byte[] modifiedClassBytes = modifyClasses(sourceClassBytes, classNameAnalytics)
                if (modifiedClassBytes) {
                    modified = new File(tempDir, className.replace('.', '') + '.class')
                    if (modified.exists()) {
                        modified.delete()
                    }
                    modified.createNewFile()
                    outputStream = new FileOutputStream(modified)
                    outputStream.write(modifiedClassBytes)
                }
            } else {
                return classFile
            }
        } catch (Exception e) {
            e.printStackTrace()
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close()
                }
            } catch (Exception e) {
                //ignore
            }
        }
        return modified
    }

    private String path2ClassName(String pathName) {
        pathName.replace(File.separator, ".").replace(".class", "")
    }
}