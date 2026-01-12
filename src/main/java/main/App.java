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

public class App {
    private App() {
    }

    private static final String INPUT_USERS_FIELD = "input/database/users.json";

    private static final ObjectWriter WRITER =
            new ObjectMapper().writer().withDefaultPrettyPrinter();

    public static void run(final String inputPath, final String outputPath) {
        List<ObjectNode> outputs = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        try {
            Database.getInstance().reset();

            File usersFile = new File(INPUT_USERS_FIELD);
            if (usersFile.exists()) {
                List<User> users = mapper.readValue(usersFile, new TypeReference<List<User>>() {});
                Database.getInstance().setUsers(users);
            }

            JsonNode commandsNode = mapper.readTree(new File(inputPath));

            if (commandsNode.isArray()) {
                for (JsonNode commandNode : commandsNode) {
                    String commandName = commandNode.get("command").asText();

                    if ("lostInvestors".equals(commandName)) {
                        break;
                    }

                    ICommand command = CommandFactory.createCommand(commandName, commandNode);

                    if (command != null) {
                        ObjectNode result = command.execute();

                        if (result != null) {
                            boolean isError = result.has("error") ||
                                    (result.has("status") && "error".equals(result.get("status").asText()));
                            boolean isViewCommand = commandName.startsWith("view");

                            if (isError || isViewCommand) {
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
}