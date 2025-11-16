package com.spulido.tfg.domain;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Field;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Getter
@Setter
@Accessors(chain = true)
public abstract class BaseEntity {
    @Id
    private String id;

    @Field
    private LocalDateTime createdAt;

    @Field
    private LocalDateTime updatedAt;
}
