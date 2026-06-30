package dev.fragmentcode.loader.classloader;

import dev.fragmentcode.api.transform.ClassTransformer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;

/**
 * Кастомный ClassLoader, через который проходит ВСЯ загрузка классов
 * Minecraft и его библиотек. Это сердце loader'а - именно здесь происходит
 * перехват байткода перед тем как JVM создаст из него Class.
 *
 * Расширяем URLClassLoader, чтобы получить готовую логику чтения .class
 * файлов из jar-архивов (через URLClassPath), но переопределяем findClass,
 * чтобы вставить transformer между "прочитать байты" и "définir класс".
 *
 * Поток для каждого загружаемого класса:
 *   1. JVM просит loadClass("net.minecraft.client.MinecraftClient")
 *   2. findClass читает оригинальные байты класса из jar'а через super
 *      (на самом деле берём байты иначе - см. readClassBytes)
 *   3. Байты передаются в classTransformer.transform(...)
 *   4. Получившиеся (возможно изменённые) байты передаются в defineClass,
 *      и именно ЭТОТ класс возвращается JVM
 *
 * classTransformer по умолчанию ClassTransformer.NO_OP (см. fragment-api) -
 * пока fragment-transformer не подключён, классы проходят без изменений.
 */
public final class FragmentClassLoader extends URLClassLoader {

    private ClassTransformer classTransformer;

    public FragmentClassLoader(List<Path> classpath, ClassTransformer classTransformer, ClassLoader parent) {
        super("FragmentClassLoader", toUrls(classpath), parent);
        this.classTransformer = classTransformer;
    }

    public FragmentClassLoader(List<Path> classpath, ClassLoader parent) {
        this(classpath, ClassTransformer.NO_OP, parent);
    }

    /**
     * Позволяет подключить transformer ПОСЛЕ создания classloader'а —
     * нужно, потому что MixinClassTransformer сам требует ссылку на
     * FragmentClassLoader (для вычисления ASM stack map frames через
     * правильный classloader), и эту ссылку можно получить только
     * после того как FragmentClassLoader уже создан. Без этого сеттера
     * было бы циклическое требование "создай A, чтобы передать в B,
     * чтобы передать обратно в A".
     */
    public void setClassTransformer(ClassTransformer classTransformer) {
        this.classTransformer = classTransformer;
    }

    /**
     * Добавляет mod jar в classpath этого classloader'а - используется
     * ModDiscovery, чтобы классы мода (включая &#64;Mixin-классы) были
     * доступны для загрузки наравне с классами игры/библиотек.
     *
     * URLClassLoader.addURL защищён (protected) - этот метод просто
     * делает его доступным извне пакета classloader.
     */
    public void addModJar(Path modJar) {

        try {
            addURL(modJar.toUri().toURL());
        } catch (java.net.MalformedURLException e) {
            throw new IllegalArgumentException("Invalid mod jar path: " + modJar, e);
        }

    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {

        byte[] originalBytes = readClassBytes(name);

        if (originalBytes == null) {
            // Класс не наш (например, часть самой JVM или не найден в
            // classpath) - отдаём поиск стандартному механизму URLClassLoader.
            return super.findClass(name);
        }

        byte[] transformedBytes = classTransformer.transform(name, originalBytes);

        return defineClass(name, transformedBytes, 0, transformedBytes.length);

    }

    /**
     * Читает сырые байты .class файла из classpath (через ресурсы
     * URLClassLoader), не определяя класс. Возвращает null, если
     * класса с таким именем нет ни в одном jar'е classpath.
     */
    private byte[] readClassBytes(String className) throws ClassNotFoundException {

        String resourcePath = className.replace('.', '/') + ".class";

        try (InputStream in = super.getResourceAsStream(resourcePath)) {

            if (in == null) {
                return null;
            }

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            in.transferTo(buffer);

            return buffer.toByteArray();

        } catch (IOException e) {
            throw new ClassNotFoundException("Failed to read class bytes for " + className, e);
        }

    }

    private static URL[] toUrls(List<Path> paths) {

        URL[] urls = new URL[paths.size()];

        for (int i = 0; i < paths.size(); i++) {

            try {
                urls[i] = paths.get(i).toUri().toURL();
            } catch (java.net.MalformedURLException e) {
                throw new IllegalArgumentException("Invalid classpath entry: " + paths.get(i), e);
            }

        }

        return urls;

    }

}
