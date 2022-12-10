package it.philipkrauss.proxysystem.provider.insults;

import com.google.common.collect.Lists;
import it.philipkrauss.proxysystem.ProxySystem;
import it.philipkrauss.proxysystem.database.DatabaseAdapter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class InsultProvider {

    public static InsultProvider create() {
        return new InsultProvider();
    }

    private final ProxySystem instance;
    private final DatabaseAdapter databaseAdapter;

    private final List<String> words = Lists.newArrayList();

    private InsultProvider() {
        this.instance = ProxySystem.getInstance();
        this.databaseAdapter = instance.getDatabaseAdapter();
        this.loadWords();
    }

    private void loadWords() {
        try (Connection connection = this.databaseAdapter.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM chatfilter_insults");
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                this.words.add(resultSet.getString("WORD"));
            }
            instance.getLogger().info(String.format("[InsultProvider] Loaded %s insult-words from database", this.words.size()));
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public List<String> getWords() {
        return words;
    }

}
