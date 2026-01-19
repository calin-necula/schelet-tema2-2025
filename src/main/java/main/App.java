package main;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cod.command.CommandFactory;
import cod.command.ICommand;
import cod.database.Database;
import cod.model.User;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class App {
    private App() {
    }

    private static final String INPUT_USERS_FIELD = "input/database/users.json";

    private static final ObjectWriter WRITER =
            new ObjectMapper().writer().withDefaultPrettyPrinter();

    /**
     Runs the application processing logic.
     */
    public static void run(final String inputPath, final String outputPath) {
        List<ObjectNode> outputs = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        try {
            Database.getInstance().reset();

            File usersFile = new File(INPUT_USERS_FIELD);
            if (usersFile.exists()) {
                List<User> users = mapper.readValue(usersFile,
                        new TypeReference<List<User>>() { });
                Database.getInstance().setUsers(users);
            }

            JsonNode commandsNode = mapper.readTree(new File(inputPath));

            if (commandsNode.isArray()) {
                for (JsonNode commandNode : commandsNode) {
                    if (!commandNode.has("command")) {
                        continue;
                    }
                    String commandName = commandNode.get("command").asText();

                    if ("lostInvestors".equals(commandName)) {
                        break;
                    }

                    ICommand command = CommandFactory.createCommand(commandName, commandNode);

                    if (command != null) {
                        ObjectNode result = command.execute();

                        if (result != null) {
                            boolean isError = result.has("error")
                                    || (result.has("status")
                                    && "error".equals(result.get("status").asText()));
                            boolean isView = commandName.startsWith("view");
                            boolean isSearch = "search".equals(commandName);
                            boolean isImpact = "generateCustomerImpactReport".equals(commandName);
                            boolean isRisk = "generateTicketRiskReport".equals(commandName);
                            boolean isEff = "generateResolutionEfficiencyReport".equals(commandName);
                            boolean isStability = "appStabilityReport".equals(commandName);
                            boolean isPerf = "generatePerformanceReport".equals(commandName);

                            if (isError || isView || isSearch || isImpact
                                    || isRisk || isEff || isStability || isPerf) {
                                outputs.add(result);
                            }
                        }
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            File outputFile = new File(outputPath);
            outputFile.getParentFile().mkdirs();
            WRITER.withDefaultPrettyPrinter().writeValue(outputFile, outputs);
        } catch (IOException e) {
            System.out.println("error writing to output file: " + e.getMessage());
        }
    }

    /**
     Main entry point of the application.
     */
    public static void main(final String[] args) {
        if (args.length < 2) {
            return;
        }
        run(args[0], args[1]);
    }
}
