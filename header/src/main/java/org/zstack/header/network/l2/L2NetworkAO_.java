package org.zstack.header.network.l2;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(L2NetworkAO.class)
public class L2NetworkAO_ {
    public static volatile SingularAttribute<L2NetworkAO, String> uuid;
    public static volatile SingularAttribute<L2NetworkAO, String> name;
    public static volatile SingularAttribute<L2NetworkAO, String> type;
    public static volatile SingularAttribute<L2NetworkAO, String> description;
    public static volatile SingularAttribute<L2NetworkAO, String> zoneUuid;
    public static volatile SingularAttribute<L2NetworkAO, String> physicalInterface;
    public static volatile SingularAttribute<L2NetworkAO, Timestamp> createDate;
    public static volatile SingularAttribute<L2NetworkAO, Timestamp> lastOpDate;
}
