package com.seb.dependencyextractor;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.shared.invoker.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * Created by sv on 23/06/2015.
 */

//TODO extract in a file + multithread + tell the original project(s)
//TODO Difference between two files
public class MavenDependencyExtractor implements Runnable {
    private final Invoker invoker;

    public MavenDependencyExtractor() {
        invoker = new DefaultInvoker();
        invoker.setOutputHandler(null);
    }

    public static void main(String JavaLatte[]) throws IOException, InterruptedException, MavenInvocationException {
        MavenDependencyExtractor mavenDependencyExtractor = new MavenDependencyExtractor();
//        mavenDependencyExtractor.extractDependencies("C:/svn/trunk/projects/pacs");

        mavenDependencyExtractor.listAllDependencies("C:/svn/trunk/projects");
    }

    private Collection<File> collectPomFilesInDirectory(File sourceDirectory) {
        System.out.println("Collecting poms");
        return FileUtils.listFiles(sourceDirectory, new RegexFileFilter("pom\\.xml"), DirectoryFileFilter.DIRECTORY);
    }

    private void executeMavenGoals(File pomfile) throws MavenInvocationException {
        System.out.println("Executing maven goals for " + pomfile.getAbsolutePath());
        Long startTime = System.currentTimeMillis();
        List<String> goals = Collections.singletonList("dependency:tree -DoutputFile=C:/export_dependencies/dependencies.mvn -DappendOutput=true");

        InvocationRequest request = new DefaultInvocationRequest();
        request.setGoals(goals);
        request.setPomFile(pomfile);

        invoker.execute(request);
        System.out.println("Maven goals executed in " + (System.currentTimeMillis() - startTime) + " ms");
    }

    private Set<String> formatDependencies(String dependencyFilename) {
        System.out.println("Extracting dependencies");
        Path path = Paths.get(dependencyFilename);
        Set<String> results = new LinkedHashSet<>();

        try {
            Stream<String> lines = Files.lines(path);

            lines.forEach(s -> {
                        if (!(s.contains("+-") && s.contains("com.telemis"))) {
//                            s = StringUtils.replace(s, "|  +- ", "   ");
//                            s = StringUtils.replace(s, "+- ", "   ");
//                            s = StringUtils.replace(s, "|  \\- ", "      ");
//                            s = StringUtils.replace(s, "   \\- ", "      ");
//                            s = StringUtils.replace(s, "\\- ", "   ");
//                            s = StringUtils.remove(s, ":compile");
//                            s = StringUtils.remove(s, ":runtime");
//                            s = StringUtils.remove(s, ":provided");
                            s = s.trim();

                            if (!s.contains(":test")) {
                                if (!s.contains("com.telemis")) {
                                    s = "   " + s;
                                }
                                results.add(s);
                            }
                        }
                    }
            );
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }

        return results;
    }

    private Map<String, Set<String>> cleanDependencies(String dependencyFilename) {
        System.out.println("Extracting dependencies");
        Path path = Paths.get(dependencyFilename);
        Map<String, Set<String>> results = new TreeMap<>();

        try {
            Stream<String> lines = Files.lines(path);

            lines.forEach(s -> {
                        if (!s.contains("com.telemis")) {
                            s = StringUtils.remove(s, "|");
                            s = StringUtils.remove(s, "+");
                            s = StringUtils.remove(s, "-");
                            s = StringUtils.remove(s, "\\");
                            s = StringUtils.remove(s, ":compile");
                            s = StringUtils.remove(s, ":runtime");
                            s = StringUtils.remove(s, ":provided");
                            s = StringUtils.remove(s, ":test");
                            s = s.trim();

//                            final String finalS = s;
//                            boolean exists = CollectionUtils.exists(results, new Predicate<String>() {
//                                @Override
//                                public boolean evaluate(String element) {
//                                    String dependencyWithoutVersion = finalS.substring(0, StringUtils.lastIndexOf(finalS, ":") - 1);
//                                    String elementWithoutVersion = element.substring(0, StringUtils.lastIndexOf(element, ":") - 1);
//
//                                    return dependencyWithoutVersion.equals(elementWithoutVersion);
//                                }
//                            });

//                            if(exists){
//                                s = s + " => SOME VERSIONS !!!";
//                            }

                            String dependencyWithoutVersion = s.substring(0, StringUtils.lastIndexOf(s, ":"));
                            String dependencyVersion = s.substring(StringUtils.lastIndexOf(s, ":") + 1);

                            if (!results.containsKey(dependencyWithoutVersion)) {
                                results.put(dependencyWithoutVersion, new TreeSet<>());
                            }

                            results.get(dependencyWithoutVersion).add(dependencyVersion);
                        }
                    }
            );
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }

        return results;
    }

    public void listAllDependencies(String sourceDirectoryName) throws MavenInvocationException, InterruptedException, IOException {
        Long startTime = System.currentTimeMillis();
        cleanTempDirectory(new File("C:/export_dependencies"));
        Collection<File> pomFiles = collectPomFilesInDirectory(new File(sourceDirectoryName));

        for (File pomfile : pomFiles) {
            executeMavenGoals(pomfile);
        }

        Map<String, Set<String>> results = cleanDependencies("C:/export_dependencies/dependencies.mvn");

        System.out.println("\ndependencies : ");

        for (Map.Entry<String, Set<String>> entry : results.entrySet()) {
            String versions = "{";

            for (String value : entry.getValue()) {
                versions += value + ", ";
            }

            versions = StringUtils.chop(StringUtils.chop(versions)) + "}";

            System.out.println(entry.getKey() + " => " + versions + ((versions.contains(",")) ? "          !!! ATTENTION, DIFFERENT VERSION !!!" : StringUtils.EMPTY));
        }

        System.out.println("Dependencies extracted in " + ((System.currentTimeMillis() - startTime) / 1000) + " s");
    }

    public void extractDependencies(String sourceDirectoryName) throws MavenInvocationException, InterruptedException, IOException {
        Long startTime = System.currentTimeMillis();
        cleanTempDirectory(new File("C:/export_dependencies"));
        Collection<File> pomFiles = collectPomFilesInDirectory(new File(sourceDirectoryName));

        for (File pomfile : pomFiles) {
            executeMavenGoals(pomfile);
        }

        Set<String> results = formatDependencies("C:/export_dependencies/dependencies.mvn");

        System.out.println("\ndependencies : ");

        results.forEach(System.out::println);

        System.out.println("\nDependencies extracted in " + ((System.currentTimeMillis() - startTime) / 1000) + " s");
    }

    public void cleanTempDirectory(File dir) throws IOException {
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();

            for (File aChildren : children) {
                aChildren.delete();
            }
        }

        dir.delete();
        Files.createDirectory(Paths.get(dir.getAbsolutePath()));
    }

    @Override
    public void run() {

    }
}
