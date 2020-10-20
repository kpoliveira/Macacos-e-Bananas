package main;

import org.apache.commons.cli.*;

public class Main {
    private static Options argsOptions() {
        Options options = new Options();
        Option number = new Option("n", "numero", true, "Numero total de bananas");
        number.setRequired(true);
        options.addOption(number);
        return options;
    }

    public static void main(String[] args) {
        Options options = argsOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            //cmd = parser.parse(options, args);
            cmd = parser.parse(options, new String[]{"-n", "111"});
        } catch (ParseException e) {
            HelpFormatter helpF = new HelpFormatter();
            System.err.println(e.getMessage());
            helpF.printHelp("MacacosBananas [opcoes]", options);
            System.exit(1);
        }
        long n = 1;
        String opt = "", val = "";
        try {
            opt = "n";
            val = cmd.getOptionValue(opt);
            n = Long.parseUnsignedLong(val);
        } catch (NumberFormatException e) {
            System.err.println("Valor " + val + " para " + opt + " invalido");
        }
        Bananeira bananeira = new Bananeira(n);
        bananeira.run();
    }
}
