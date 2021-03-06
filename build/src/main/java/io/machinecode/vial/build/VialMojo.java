package io.machinecode.vial.build;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;
import org.stringtemplate.v4.STWriter;
import org.stringtemplate.v4.misc.ErrorBuffer;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_SOURCES;

/**
 * @author <a href="mailto:brent.n.douglas@gmail.com">Brent Douglas</a>
 * @since 1.0
 */
@Mojo(name = "generate", defaultPhase = GENERATE_SOURCES, requiresProject = false)
public class VialMojo extends AbstractMojo {

    private static final String UTF_8 = "UTF-8";

    /**
     * The root directory containing templates.
     */
    @Parameter(required = true)
    File in;

    /**
     * The root directory to output the templates to.
     */
    @Parameter(required = true)
    File out;

    private static final String[][] PROPERTIES = {
            { "B", "Byte", "byte", "(int)key", "(int)value", " == " },
            { "S", "Short", "short", "(int)key", "(int)value", " == " },
            { "I", "Integer", "int", "key", "value", " == " },
            { "L", "Long", "long", "(int)(key ^ (key >>> 32))", "(int)(value ^ (value >>> 32))", " == " },
            { "C", "Character", "char", "(int)key", "(int)value", " == " }//,
            //{ "O", "Object", "Object", "key.hashCode()", ").equals(" }
    };

    private enum Match {
        ALL('A'),
        MATCHING('M'),
        DIFFERENT('D');

        private final char prefix;

        private Match(final char prefix) {
            this.prefix = prefix;
        }

        static Match of(final char prefix) {
            switch (prefix) {
                case 'A': return ALL;
                case 'M': return MATCHING;
                case 'D': return DIFFERENT;
            }
            throw new IllegalStateException("No match type with prefix " + prefix);
        }
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        assert in != null && in.isDirectory();
        assert out != null;

        if (!out.isDirectory() && !out.mkdirs()) {
            throw new MojoExecutionException("Could not create directory " + out.getAbsolutePath());
        }

        final ErrorBuffer errors = new ErrorBuffer();
        _package(in, out, errors);
        if (!errors.errors.isEmpty()) {
            throw new MojoExecutionException(errors.toString());
        }
    }

    private static void _package(final File in, final File out, final ErrorBuffer errors) throws MojoExecutionException {
        final File[] files = in.listFiles();
        if (files == null) {
            return;
        }
        for (final File file : files) {
            final String name = file.getName();
            if (file.isDirectory()) {
                _package(file, new File(out, name), errors);
                continue;
            }
            if (!out.isDirectory() && !out.mkdirs()) {
                throw new MojoExecutionException("Could not create directory " + out.getAbsolutePath());
            }
            if (name.length() < 7 || !name.endsWith(".stg")) {
                throw new MojoExecutionException("Invalid template " + file.getAbsolutePath());
            }
            final STGroup group = new STGroupFile(file.getAbsolutePath(), UTF_8);
            group.setListener(errors);
            if (!errors.errors.isEmpty()) {
                throw new MojoExecutionException(errors.toString());
            }
            final String[] parts = name.split("_");
            if (parts.length != 3) {
                continue;
            }
            final Match match = Match.of(parts[0].charAt(0));

            switch (parts[1].length()) {
                case 1:
                    col(group, out, file, errors, parts[1], parts[2].substring(0, parts[2].length() - 4));
                    break;
                case 2:
                    map(group, out, file, errors, match, parts[1], parts[2].substring(0, parts[2].length() - 4));
                    break;
                default:
                    continue;
            }
        }
    }

    private static void col(final STGroup group, final File out, final File file, final ErrorBuffer errors, final String prefix, final String name) throws MojoExecutionException {
        final char f = prefix.charAt(0);
        final String tn = prefix + name;
        for (final String[] k : PROPERTIES) {
            final char ki = k[0].charAt(0);
            if (f == 'P') {
                final ST st = group.getInstanceOf(tn);
                if (st == null) {
                    throw new MojoExecutionException("Can't find template " + tn + " in " + file.getAbsolutePath());
                }
                st.add("I", k[0]);
                st.add("P", k[1]);
                st.add("p", k[2]);
                st.add("hck", k[3]);
                st.add("hcv", k[4]);
                st.add("X", ">>");
                final String that = "" + ki + name + ".java";
                generate(st,  new File(out, that), errors);
            }
        }
    }

    private static void map(final STGroup group, final File out, final File file, final ErrorBuffer errors, final Match match, final String prefix, final String name) throws MojoExecutionException {
        final char f = prefix.charAt(0);
        final char s = prefix.charAt(1);
        final String tn = prefix + name;
        for (final String[] k : PROPERTIES) {
            final char ki = k[0].charAt(0);
            if (f == 'P' && s == 'P') {
                switch (match) {
                    case ALL: {
                        for (final String[] v : PROPERTIES) {
                            final ST st = group.getInstanceOf(tn);
                            if (st == null) {
                                throw new MojoExecutionException("Can't find template " + tn + " in " + file.getAbsolutePath());
                            }
                            st.add("Ik", k[0]);
                            st.add("Pk", k[1]);
                            st.add("pk", k[2]);
                            st.add("hck", k[3]);
                            st.add("Iv", v[0]);
                            st.add("Pv", v[1]);
                            st.add("pv", v[2]);
                            st.add("hcv", v[4]);
                            st.add("X", ">>");
                            final char vi = v[0].charAt(0);
                            final String that = "" + ki + vi + name + ".java";
                            generate(st,  new File(out, that), errors);
                        }
                        break;
                    }
                    case MATCHING: {
                        final ST st = group.getInstanceOf(tn);
                        if (st == null) {
                            throw new MojoExecutionException("Can't find template " + tn + " in " + file.getAbsolutePath());
                        }
                        st.add("I", k[0]);
                        st.add("P", k[1]);
                        st.add("p", k[2]);
                        st.add("hck", k[3]);
                        st.add("hcv", k[4]);
                        st.add("X", ">>");
                        final String that = "" + ki + ki + name + ".java";
                        generate(st, new File(out, that), errors);
                        break;
                    }
                    case DIFFERENT: {
                        for (final String[] v : PROPERTIES) {
                            if (v[0].equals(k[0])) {
                                continue;
                            }
                            final ST st = group.getInstanceOf(tn);
                            if (st == null) {
                                throw new MojoExecutionException("Can't find template " + tn + " in " + file.getAbsolutePath());
                            }
                            st.add("Ik", k[0]);
                            st.add("Pk", k[1]);
                            st.add("pk", k[2]);
                            st.add("hck", k[3]);
                            st.add("Iv", v[0]);
                            st.add("Pv", v[1]);
                            st.add("pv", v[2]);
                            st.add("hcv", v[4]);
                            st.add("X", ">>");
                            final char vi = v[0].charAt(0);
                            final String that = "" + ki + vi + name + ".java";
                            generate(st,  new File(out, that), errors);
                        }
                        break;
                    }
                }
            } else if (f == 'P') {
                final ST st = group.getInstanceOf(tn);
                if (st == null) {
                    throw new MojoExecutionException("Can't find template " + tn + " in " + file.getAbsolutePath());
                }
                st.add("I", k[0]);
                st.add("P", k[1]);
                st.add("p", k[2]);
                st.add("hck", k[3]);
                st.add("hcv", k[4]);
                st.add("X", ">>");
                final String that = "" + ki + s + name + ".java";
                generate(st,  new File(out, that), errors);
            } else if (s == 'P') {
                final ST st = group.getInstanceOf(tn);
                if (st == null) {
                    throw new MojoExecutionException("Can't find template " + tn + " in " + file.getAbsolutePath());
                }
                st.add("I", k[0]);
                st.add("P", k[1]);
                st.add("p", k[2]);
                st.add("hck", k[3]);
                st.add("hcv", k[4]);
                st.add("X", ">>");
                final String that = "" + f + ki + name + ".java";
                generate(st,  new File(out, that), errors);
            }
        }
    }

    private static void generate(final ST st, final File out, final ErrorBuffer errors) throws MojoExecutionException {
        try {
            st.write(out, errors, UTF_8, Locale.ENGLISH, STWriter.NO_WRAP);
            if (!errors.errors.isEmpty()) {
                throw new MojoExecutionException(errors.toString());
            }
        } catch (final IOException e) {
            throw new MojoExecutionException("Could not write file " +  out.getAbsolutePath(), e);
        }
    }
}
