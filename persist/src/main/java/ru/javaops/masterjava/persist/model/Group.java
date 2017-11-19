package ru.javaops.masterjava.persist.model;

import com.bertoncelj.jdbi.entitymapper.Column;
import lombok.*;
import ru.javaops.masterjava.persist.model.types.GroupType;

@Data
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Group extends BaseEntity {
    @NonNull
    private String name;
    @NonNull
    private GroupType type;
    @Column("project_id")
    @NonNull
    private Project project;

    public Group(Integer id, String name, GroupType type, Project project) {
        this(name, type, project);
        this.id = id;
    }
}