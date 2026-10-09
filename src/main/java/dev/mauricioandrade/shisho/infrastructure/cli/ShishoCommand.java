package dev.mauricioandrade.shisho.infrastructure.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/** {@code shisho}: without a subcommand, shows the usage. */
@Command(name = "shisho", mixinStandardHelpOptions = true, versionProvider = ShishoCommand.ManifestVersion.class)
public final class ShishoCommand implements Runnable {

    @Spec
    private CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }

    /** {@code Implementation-Version} from the jar manifest; {@code dev} when running from classes. */
    public static String version() {
        String version = ShishoCommand.class.getPackage().getImplementationVersion();
        return version == null ? "dev" : version;
    }

    static final class ManifestVersion implements IVersionProvider {

        @Override
        public String[] getVersion() {
            return new String[] {"Shishō " + version()};
        }
    }
}
