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

    /** {@code Implementation-Version} from the jar manifest; absent when running from classes. */
    static final class ManifestVersion implements IVersionProvider {

        @Override
        public String[] getVersion() {
            String version = ShishoCommand.class.getPackage().getImplementationVersion();
            return new String[] {"Shishō " + (version == null ? "dev" : version)};
        }
    }
}
