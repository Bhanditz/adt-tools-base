//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.11 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.04 at 02:23:40 PM PST 
//


package com.android.repository.impl.generated.v1;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import com.android.repository.api.Dependency;


/**
 * 
 *                 A list of dependencies.
 *             
 * 
 * <p>Java class for dependenciesType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="dependenciesType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="dependency" type="{http://schemas.android.com/repository/android/common/01}dependencyType" maxOccurs="unbounded"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "dependenciesType", propOrder = {
    "dependency"
})
@SuppressWarnings({
    "override",
    "unchecked"
})
public class DependenciesType
    extends com.android.repository.impl.meta.RepoPackageImpl.Dependencies
{

    @XmlElement(required = true)
    protected List<DependencyType> dependency;

    /**
     * Gets the value of the dependency property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the dependency property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDependency().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DependencyType }
     * 
     * 
     */
    public List<DependencyType> getDependencyInternal() {
        if (dependency == null) {
            dependency = new ArrayList<DependencyType>();
        }
        return this.dependency;
    }

    public List<Dependency> getDependency() {
        return ((List) getDependencyInternal());
    }

    public ObjectFactory createFactory() {
        return new ObjectFactory();
    }

}
