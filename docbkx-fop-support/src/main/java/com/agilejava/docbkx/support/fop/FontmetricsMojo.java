package com.agilejava.docbkx.support.fop;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.fop.fonts.apps.PFMReader;
import org.apache.fop.fonts.apps.TTFReader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;

/**
 * A Maven plugin for generating FOP font metrics files from font files.
 * 
 * 
 * @author Wilfred Springer
 * @goal generate
 */
public class FontmetricsMojo extends AbstractMojo {

    /**
     * The list of all {@link MetricsFileBuilder MetricsFileBuilders} to be
     * included.
     */
    private MetricsFileBuilder[] builders = new MetricsFileBuilder[] {
            new Type1MetricsFileBuilder(), new TtfMetricsFileBuilder() };

    /**
     * The directory containing the font files.
     * 
     * @parameter expression="${basedir}/src/fonts"
     */
    private File sourceDirectory;

    /**
     * The directory to which the metrics files will be generated.
     * 
     * @parameter expression="${basedir}/target/fonts"
     */
    private File targetDirectory;

    /**
     * Creates a WinAnsi-encoded font metrics file. Without this option, a
     * CID-keyed font metrics file is created. The table below summarizes the
     * differences between these two encoding options as currently used within
     * FOP. Please note that this information only applies to TrueType fonts and
     * TrueType collections
     * 
     * @parameter
     */
    private boolean ansi;

    /**
     * {@inheritDoc} Generates font metric files from the font files found in
     * {@link #sourceDirectory the source directory}.
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        String[] fontFiles = getFontFiles();
        targetDirectory.mkdirs();
        for (int i = 0; i < fontFiles.length; i++) {
            String fontFile = fontFiles[i];
            for (int j = 0; j < builders.length; j++) {
                if (builders[j].matches(fontFile)) {
                    transform(new File(sourceDirectory, fontFile)
                            .getAbsolutePath(), builders[j]);
                    break;
                }
            }
        }
    }

    /**
     * Transforms the font file passed in using the given builder.
     * 
     * @param fontFile
     *            The font file to be transformed into a metrics file.
     * @param builder
     *            The builder that's going to do it.
     * @throws MojoExecutionException
     *             If the builder somehow fails to do it.
     */
    private static void transform(String fontFile, MetricsFileBuilder builder)
            throws MojoExecutionException {
        try {
            builder.transform(fontFile);
        } catch (IOException ioe) {
            throw new MojoExecutionException("Failed to transform " + fontFile,
                    ioe);
        }
    }

    /**
     * Returns the target metrics file for the given font file.
     * 
     * @param fontFile
     *            The file to be transformed.
     * @return The name of the target metrics file.
     */
    private String getTargetFile(String fontFile) {
        StringBuilder builder = new StringBuilder();
        String basename = FileUtils.basename(fontFile);
        builder.append(basename.substring(0, basename.length() - 1));
        builder.append("-metrics.xml");
        File file = new File(targetDirectory, builder.toString());
        return file.getAbsolutePath();
    }

    /**
     * Returns an array of the names of all font files found.
     * 
     * @return An array with the names of all font files found.
     */
    private String[] getFontFiles() {
        String[] includes = getFontFileIncludes();
        getLog().debug("Patterns " + Arrays.asList(includes));
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(sourceDirectory);
        scanner.setIncludes(includes);
        scanner.scan();
        String[] results = scanner.getIncludedFiles();
        if (getLog().isDebugEnabled()) {
            for (int i = 0; i < results.length; i++) {
                getLog().debug("Found " + results[i]);
            }
            getLog().debug("Found " + results.length + " font files in total.");
        }
        return results;
    }

    /**
     * Returns a list of patterns of font files to be included while scanning
     * for font files.
     * 
     * @return A list of patterns matching font files to be included while
     *         scanning for other font files.
     */
    private String[] getFontFileIncludes() {
        List results = new ArrayList();
        for (int i = 0; i < builders.length; i++) {
            builders[i].appendSuffixes(results);
        }
        return (String[]) results.toArray(new String[0]);
    }

    private interface MetricsFileBuilder {

        boolean matches(String fontFile);

        void transform(String fontFile) throws IOException;

        void appendSuffixes(List list);

    }

    private class Type1MetricsFileBuilder implements MetricsFileBuilder {

        public boolean matches(String fontFile) {
            return fontFile.toLowerCase().endsWith(".pfm");
        }

        public void transform(String fontFile) throws IOException {
            String[] args = new String[2];
            args[0] = fontFile;
            args[1] = getTargetFile(fontFile);
            PFMReader.main(args);
        }

        public void appendSuffixes(List list) {
            list.add("*.pfm");
            list.add("*.PFM");
        }

    }

    private class TtfMetricsFileBuilder implements MetricsFileBuilder {

        public boolean matches(String fontFile) {
            return fontFile.toLowerCase().endsWith(".ttf");
        }

        public void transform(String fontFile) throws IOException {
            String[] args;
            if (ansi) {
                args = new String[4];
                args[0] = "-enc";
                args[1] = "ansi";
                args[2] = fontFile;
                args[3] = getTargetFile(fontFile);
            }
            else
            {
                args = new String[2];
                args[0] = fontFile;
                args[1] = getTargetFile(fontFile);
            }
            TTFReader.main(args);
        }

        public void appendSuffixes(List list) {
            list.add("*.ttf");
            list.add("*.TTF");
        }

    }

}
