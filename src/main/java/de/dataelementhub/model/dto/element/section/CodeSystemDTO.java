package de.dataelementhub.model.dto.element.section;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
@Data
@NoArgsConstructor
@EqualsAndHashCode
public class CodeSystemDTO implements Serializable {
    private String version;
    private Integer sourceId;
}
