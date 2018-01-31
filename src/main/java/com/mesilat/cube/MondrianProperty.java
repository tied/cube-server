package com.mesilat.cube;

import com.atlassian.activeobjects.external.ActiveObjects;
import java.util.Arrays;
import mondrian.olap.MondrianProperties;
import net.java.ao.Preload;
import net.java.ao.RawEntity;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.PrimaryKey;
import net.java.ao.schema.Table;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.eigenbase.util.property.Property;

@Preload
@Table("MONDRIAN")
public interface MondrianProperty extends RawEntity<Integer> {
    @PrimaryKey
    @NotNull
    String getName();

    String getValue();
    void setValue(String value);


    static ObjectNode toNodeObject(ObjectMapper mapper, MondrianProperty[] props){
        ObjectNode result = mapper.createObjectNode();
        Arrays.asList(props).stream().forEach((prop)->{
            result.put(prop.getName(), prop.getValue());
        });
        return result;
    }
    static ObjectNode toNodeObject(ObjectMapper mapper, MondrianProperties props){
        ObjectNode result = mapper.createObjectNode();
        props.getProperties().stream().forEach((key)->{
            Property prop = (Property)key;
            if (prop.isSet()){
                result.put(prop.getPath(), prop.getString());
            }
        });
        return result;
    }
    static void load(ActiveObjects ao){
        Arrays.asList(ao.find(MondrianProperty.class)).stream().forEach((prop)->{
            MondrianProperties.instance().setProperty(prop.getName(), prop.getValue());
        });
        // Cache disabled by default
        if (MondrianProperties.instance().getProperty("mondrian.rolap.star.disableCaching") == null){
            MondrianProperties.instance().setProperty("mondrian.rolap.star.disableCaching", "true");
        }
    }
}