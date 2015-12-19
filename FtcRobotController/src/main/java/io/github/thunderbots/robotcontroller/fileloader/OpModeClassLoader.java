package io.github.thunderbots.robotcontroller.fileloader;

import android.os.Environment;

import com.qualcomm.ftcrobotcontroller.FtcRobotControllerActivity;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import dalvik.system.DexClassLoader;
import dalvik.system.DexFile;
import io.github.thunderbots.robotcontroller.logging.ThunderLog;

/**
 * The {@code OpModeClassLoader} class is responsible for loading class files into instantiated
 * objects.
 *
 * @author Zach Ohara
 */
public class OpModeClassLoader {

    private static ClassLoader classLoader;
    private static List<Class<? extends OpMode>> opModeList;

    public static final String FILE_LOCATION = "FIRST";

    public static List<Class<? extends OpMode>> loadJars(List<File> fileList) {
        URL[] jarurls = getJarURLs(fileList);
        classLoader = getClassLoader(jarurls);
        Thread.currentThread().setContextClassLoader(classLoader);
        opModeList = new ArrayList<Class<? extends OpMode>>();
        for (File jarfile : fileList) {
            if (jarfile.getName().endsWith(".jar")) {
                try {
                    loadJarFile(jarfile);
                } catch (Exception ignore) {}
            }
        }
        return opModeList;
    }

    private static ClassLoader getClassLoader(URL[] jarurls) {
        String pathString = getDelimitedPathString(jarurls);
        File cacheFile = new File(FtcRobotControllerActivity.getPrivateFilesDirectory(), "/thunderbots/");
        cacheFile.mkdirs();
        String cacheDir = cacheFile.toString();
        ClassLoader parentLoader = OpModeClassLoader.class.getClassLoader();
        return new DexClassLoader(pathString, cacheDir, null, parentLoader);
    }

    /**
     * Returns a single string that contains the value of toString() for every object given
     * in the array, delimited by {@code java.io.File#pathSeperator}
     *
     * @param arr the array of objects.
     * @param <T> the type of the array. This doesn't really matter at all.
     * @return the concatenation of o.toString() for every object o in arr.
     */
    private static <T> String getDelimitedPathString(T[] arr) {
        String result = "";
        for (T obj : arr){
            result += File.pathSeparator;
            result += obj.toString();
        }
        return result.substring(1);
    }

    /**
     * Loads the classes contained in a given jar file. The jar file must have already been converted
     * to a dalvik-compatible form.
     *
     * @param jarfile the file to load classefs from.
     * @throws IOException if an IOException is thrown by the underlying class loading system.
     */
    private static void loadJarFile(File jarfile) throws IOException {
        File cache = new File(FtcRobotControllerActivity.getPrivateFilesDirectory(), "/thunderbots/temp");
        DexFile jarobj = DexFile.loadDex(jarfile.getAbsolutePath(), cache.getAbsolutePath(), 0);
        Enumeration<String> jarentries = jarobj.entries();
        while (jarentries.hasMoreElements()) {
            String entry = jarentries.nextElement();
            try {
                Class<?> c = classLoader.loadClass(entry);
                attemptLoadClass(c);
            }
            catch (Throwable ex) {

            }
        }
        jarobj.close();
    }

    /**
     * Attempts to load the given class and any nested classes recursively. Any exception thrown from
     * within this method will be caught and handled (either ignored or logged).
     * <br>
     * The class will be loaded by the {@code classLoader} class variable. If the loaded class is
     * a valid, instantiable {@code OpMode}, it will be added to the {@code opModeList} class
     * variable.
     *
     * @param c the Class of the opmode to be loaded
     */
    private static void attemptLoadClass(Class<?> c) {
        attemptLoadOpMode(c);
        for (Class<?> i : c.getDeclaredClasses())
            attemptLoadClass(i);
    }

    /**
     * Attempts to instantiate the given class. If the class is instantiable and a subclass of OpMode,
     * it will be added to opModeList
     *
     * @param c the Class to be tested and added to opModeList
     */
    @SuppressWarnings("unchecked")
    private static void attemptLoadOpMode(Class<?> c) {
        try {
            if (OpMode.class.isAssignableFrom(c) && attemptInstantiate((Class<OpMode>) c)) {
                ThunderLog.i("Found " + c.getSimpleName() + " as an op mode");
                opModeList.add((Class<OpMode>) c);
            }
        } catch (Throwable ignore) {

        }
    }

    /**
     * Tests the instantiability of the given class
     *
     * @param c the class to be tested
     * @return whether or not the class is instantiable
     */
    private static boolean attemptInstantiate(Class<? extends OpMode> c) {
        try {
            Object instance = c.newInstance();
        } catch (IllegalAccessException ex) {
            return false;
        } catch (InstantiationException ex) {
            return false;
        }
        return true;
    }

    private static URL[] getJarURLs(List<File> fileList) {
        List<URL> jarList = new ArrayList<URL>();
        for (File f : fileList) {
            if (f.isFile() && f.getName().endsWith(".jar")) {
                try {
                    jarList.add(f.getAbsoluteFile().toURI().toURL());
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        }
        return jarList.toArray(new URL[jarList.size()]);
    }

    /**
     * Gets a list of all the files contained within the app's directory, usually "sdcard/FIRST/"
     *
     * @return a list of all files in the apps directory.
     */
    public static List<File> getFileSet() {
        List<File> fileList = new ArrayList<File>();
        getFilesInDirectory(getTargetDirectory(), fileList);
        return fileList;
    }

    /**
     * Recursively browses the given directory and builds a list that contains every file
     * in the given directory.
     *
     * @param current the directory to look for files in.
     * @param fileList the list of all files in the given directory. This list should be
     * empty when
     */
    private static void getFilesInDirectory(File current, List<File> fileList) {
        for (File f : current.listFiles()) {
            if (f.isFile()) {
                fileList.add(f);
            } else if (f.isDirectory()) {
                getFilesInDirectory(f, fileList);
            }
        }
    }

    /**
     * Gets the {@code File} object for the directory that should be searched for jar files.
     *
     * @return the {@code File} to search.
     */
    private static File getTargetDirectory() {
        File sdcard = Environment.getExternalStorageDirectory();
        return new File(sdcard, FILE_LOCATION);
    }

}
