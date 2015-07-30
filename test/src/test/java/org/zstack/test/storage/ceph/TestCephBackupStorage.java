package org.zstack.test.storage.ceph;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.simulator.storage.backup.sftp.SftpBackupStorageSimulatorConfig;
import org.zstack.storage.backup.sftp.APIQuerySftpBackupStorageMsg;
import org.zstack.storage.backup.sftp.APIQuerySftpBackupStorageReply;
import org.zstack.storage.backup.sftp.SftpBackupStorageInventory;
import org.zstack.storage.ceph.backup.CephBackupStorageMonVO;
import org.zstack.storage.ceph.backup.CephBackupStorageVO;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.search.QueryTestValidator;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

public class TestCephBackupStorage {
    CLogger logger = Utils.getLogger(TestCephBackupStorage.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    GlobalConfigFacade gcf;
    SessionInventory session;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/ceph/TestCephBackupStorage.xml", con);
        deployer.addSpringConfig("ceph.xml");
        deployer.addSpringConfig("cephSimulator.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        gcf = loader.getComponent(GlobalConfigFacade.class);
        session = api.loginAsAdmin();
    }
    
    @Test
    public void test() throws ApiSenderException, InterruptedException {
        CephBackupStorageVO bs = dbf.listAll(CephBackupStorageVO.class).get(0);
        Assert.assertEquals("7ff218d9-f525-435f-8a40-3618d1772a64", bs.getFsid());
        Assert.assertEquals(SizeUnit.TERABYTE.toByte(1), bs.getTotalCapacity());
        Assert.assertEquals(SizeUnit.GIGABYTE.toByte(500), bs.getAvailableCapacity());

        Assert.assertEquals(2, dbf.count(CephBackupStorageMonVO.class));
    }
}
