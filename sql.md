SELECT 
    c.owner AS referencing_owner,
    c.table_name AS referencing_table,
    c.constraint_name,
    c.constraint_type,
    CASE 
        WHEN c.constraint_type = 'R' THEN 'FOREIGN KEY'
        WHEN c.constraint_type = 'P' THEN 'PRIMARY KEY'
        WHEN c.constraint_type = 'U' THEN 'UNIQUE'
        WHEN c.constraint_type = 'C' THEN 
            CASE 
                WHEN c.search_condition LIKE '%RESOURCE_ID%' THEN 'CHECK (references RESOURCE_ID)'
                ELSE 'CHECK'
            END
        ELSE c.constraint_type
    END AS constraint_type_description,
    cc.column_name AS referencing_column,
    r.table_name AS referenced_table,
    r.constraint_name AS referenced_constraint,
    c.delete_rule,
    c.status
FROM 
    all_constraints c
JOIN 
    all_cons_columns cc ON c.constraint_name = cc.constraint_name AND c.owner = cc.owner
LEFT JOIN 
    all_constraints r ON c.r_constraint_name = r.constraint_name AND c.r_owner = r.owner
WHERE 
    (
        -- Foreign keys referencing resource.resource_id
        (c.constraint_type = 'R' AND r.table_name = 'RESOURCE' AND r.constraint_type IN ('P', 'U'))
        OR
        -- Any constraint that explicitly uses RESOURCE_ID column
        (cc.column_name = 'RESOURCE_ID' AND c.table_name != 'RESOURCE')
        OR
        -- Check constraints that mention RESOURCE_ID in their condition
        (c.constraint_type = 'C' AND UPPER(c.search_condition) LIKE '%RESOURCE_ID%')
    )
    -- Optional: filter by current schema only
    -- AND c.owner = USER
ORDER BY 
    c.owner, 
    c.table_name, 
    c.constraint_type,
    c.constraint_name;