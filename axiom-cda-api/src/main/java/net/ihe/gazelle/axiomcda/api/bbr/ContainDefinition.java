package net.ihe.gazelle.axiomcda.api.bbr;



import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;


/**
 * Contain definition used at flattenization 2nd step (flatten 1), that will then be transformed into RuleDefinition at flattenization 3rd step
 * (flatten bis).
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ContainDefinition", propOrder = {
        "item",
        "letOrAssertOrReport"
})
@XmlRootElement(name = "ContainDefinitionType")
public class ContainDefinition implements HasParent {

    private Item item;

    @XmlAttribute(name = "ref", required = true)
    private String ref;

    @XmlAttribute(name = "isMandatory")
    private Boolean isMandatory;

    //FIXME [ceoche] missing conformance attribute

    @XmlAttribute(name = "minimumMultiplicity")
    private Integer minimumMultiplicity;

    @XmlAttribute(name = "maximumMultiplicity")
    private String maximumMultiplicity;

    @XmlAttribute(name = "contain")
    private Boolean contain = true;

    @XmlTransient
    private Object parentObject;

    @XmlElements({
            @XmlElement(name = "let", type = Let.class),
            @XmlElement(name = "assert", type = Assert.class),
            @XmlElement(name = "report", type = Report.class),
            @XmlElement(name = "defineVariable", type = DefineVariable.class),
            @XmlElement(name = "element", type = RuleDefinition.class),
            @XmlElement(name = "include", type = IncludeDefinition.class),
            @XmlElement(name = "choice", type = ChoiceDefinition.class),
            @XmlElement(name = "constraint", type = FreeFormMarkupWithLanguage.class),
            @XmlElement(name = "contain", type = ContainDefinition.class)
    })
    private List<Object> letOrAssertOrReport = new ArrayList<>();

    public Object getParentObject() {
        return parentObject;
    }

    public void setParentObject(Object parentComponent) {
        this.parentObject = parentComponent;
    }

    void afterUnmarshal(javax.xml.bind.Unmarshaller u, Object parent) {
        this.parentObject = parent;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public Boolean getIsMandatory() {
        return isMandatory;
    }

    public void setIsMandatory(Boolean isMandatory) {
        this.isMandatory = isMandatory;
    }

    public Integer getMinimumMultiplicity() {
        return minimumMultiplicity;
    }

    public void setMinimumMultiplicity(Integer minimumMultiplicity) {
        this.minimumMultiplicity = minimumMultiplicity;
    }

    public String getMaximumMultiplicity() {
        return maximumMultiplicity;
    }

    public void setMaximumMultiplicity(String maximumMultiplicity) {
        this.maximumMultiplicity = maximumMultiplicity;
    }


    public List<Object> getLetOrAssertOrReport() {
        return letOrAssertOrReport;
    }

    public void setLetOrAssertOrReport(List<Object> letOrAssertOrReport) {
        this.letOrAssertOrReport = letOrAssertOrReport;
    }

    public Boolean getMandatory() {
        return isMandatory;
    }

    public void setMandatory(Boolean mandatory) {
        isMandatory = mandatory;
    }

    public Boolean getContain() {
        return contain;
    }

    public void setContain(Boolean contain) {
        this.contain = contain;
    }
}
