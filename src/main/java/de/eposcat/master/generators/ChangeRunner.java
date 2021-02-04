package de.eposcat.master.generators;

import com.google.gson.Gson;
import de.eposcat.master.approachImpl.IDatabaseAdapter;
import de.eposcat.master.generators.data.Change;
import de.eposcat.master.model.Attribute;
import de.eposcat.master.model.AttributeType;
import de.eposcat.master.model.Page;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Runs a change file, committing all changes with the given database adapter.
 */
public class ChangeRunner {
    IDatabaseAdapter adapter;
    Gson gson;
    boolean inStreamSQLException;

    public ChangeRunner(IDatabaseAdapter adapter) {
        this.gson = new Gson();
        this.adapter = adapter;
        this.inStreamSQLException = false;
    }

    public void applyChanges(Path changesFile) throws IOException, SQLException {
        Files.readAllLines(changesFile, Charset.defaultCharset()).stream()
                .map(line -> gson.fromJson(line, Change.class))
                .forEach(change -> {
                    try {
                        //Some kind of cache for page objects??
                        List<Page> pages = adapter.findPagesByType(change.getEntity());
                        for(Page page: pages){
                            switch (change.getAction()){
                                case ADD:
                                    page.addAttribute(change.getAttribute(), new Attribute(AttributeType.String, change.getValue()));
                                    break;
                                case CHANGE:
                                    Optional<String> attrCh = page.getAttributes().keySet().stream().findFirst();
                                    attrCh.ifPresent(s -> page.getAttribute(s).setValue(change.getValue()));
                                    break;
                                case REMOVE:
                                    Optional<String> attrRm = page.getAttributes().keySet().stream().findFirst();
                                    attrRm.ifPresent(page::removeAttribute);
                                    break;
                            }

                            adapter.updatePage(page);
                        }

                    } catch (SQLException e) {
                        inStreamSQLException = true;
                        e.printStackTrace();
                    }
                });
        if(inStreamSQLException){
            throw new SQLException("Error during stream execution, inspect logs for more information");
        }
    }
}
