package ru.javaops.masterjava.persist.model;

import lombok.*;

@Data
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class City extends BaseEntity {
    @NonNull
    private String code;
    @NonNull
    private String name;

    public City(Integer id, String code, String name) {
        this(code, name);
        this.id = id;
    }
}