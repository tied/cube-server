package com.mesilat.cube;

import com.atlassian.activeobjects.external.ActiveObjects;
import java.io.IOException;
import java.io.InputStream;
import net.java.ao.DBParam;
import net.java.ao.RawEntity;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.PrimaryKey;
import org.apache.commons.io.IOUtils;

public interface CatalogData extends RawEntity<String> {
    static final String DEFAULT_NAME = "default";
    static final String DEFAULT_PATH = "/schemas/default.xml";

    @NotNull
    @PrimaryKey
    String getName();
    void setName(String name);
    byte[] getData();
    void setData(byte[] data);

    static void initDefault(ActiveObjects ao){
        ao.executeInTransaction(()->{
            byte[] catalogData = null;
            try (InputStream in = CatalogData.class.getResourceAsStream(DEFAULT_PATH)) {
                catalogData = IOUtils.toByteArray(in);
            } catch(IOException ex) {
                throw new RuntimeException(String.format("Failed to read %s", DEFAULT_PATH), ex);
            }
            CatalogData cd = ao.get(CatalogData.class, DEFAULT_NAME);
            if (cd == null){
                cd = ao.create(CatalogData.class, new DBParam("NAME", DEFAULT_NAME));
            }
            cd.setData(catalogData);
            cd.save();
            return null;
        });
    }
}