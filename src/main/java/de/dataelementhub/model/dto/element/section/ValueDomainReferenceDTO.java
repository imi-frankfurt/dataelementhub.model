package de.dataelementhub.model.dto.element.section;

import de.dataelementhub.dal.jooq.tables.pojos.CodeSystem;
import de.dataelementhub.dal.jooq.tables.pojos.ValueDomainReference;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
/**
 * ValueDomain Reference DTO.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
public class ValueDomainReferenceDTO implements Serializable {

    private String subsetUri;
    private Integer scopedIdentifierId;
    private Integer codeSystemId;

    private CodeSystemDTO codeSystem;

    public String getVersion() {
        return codeSystem != null ? codeSystem.getVersion() : null;
    }

    public Integer getSourceId() {
        return codeSystem != null ? codeSystem.getSourceId() : null;
    }

    public ValueDomainReferenceDTO(CodeSystem codeSystemEntity, ValueDomainReference valueDomainReference) {
        setSubsetUri(valueDomainReference.getSubsetUri());
        setScopedIdentifierId(valueDomainReference.getScopedidentifierId());
        setCodeSystemId(codeSystemEntity.getId());
        CodeSystemDTO csDto = new CodeSystemDTO();
        csDto.setVersion(codeSystemEntity.getVersion());
        csDto.setSourceId(codeSystemEntity.getSourceId());
        setCodeSystem(csDto);
    }
}
