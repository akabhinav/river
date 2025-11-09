package com.river.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Represents the schema of a data record
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Schema {

    /**
     * Schema name/identifier
     */
    private String name;

    /**
     * Schema version
     */
    private Integer version;

    /**
     * List of fields in this schema
     */
    @Builder.Default
    private List<Field> fields = new ArrayList<>();

    /**
     * Schema namespace (for Avro compatibility)
     */
    private String namespace;

    /**
     * Schema documentation
     */
    private String documentation;

    /**
     * Add a field to the schema
     */
    public void addField(Field field) {
        fields.add(field);
    }

    /**
     * Get a field by name
     */
    public Optional<Field> getField(String name) {
        return fields.stream()
                .filter(f -> f.getName().equals(name))
                .findFirst();
    }

    /**
     * Get all field names
     */
    public List<String> getFieldNames() {
        return fields.stream()
                .map(Field::getName)
                .toList();
    }

    /**
     * Check if schema has a field
     */
    public boolean hasField(String name) {
        return getField(name).isPresent();
    }
}
