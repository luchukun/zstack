package org.zstack.test.mevoco.billing;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.billing.*;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.HostCapacityOverProvisioningManager;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.network.service.flat.FlatNetworkServiceSimulatorConfig;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 1. start two vms and run for a while
 * 2. create prices with date = 1970
 * <p>
 * confirm the billing is correct
 * <p>
 * 3. delete the prices
 * <p>
 * confirm the billing is zero
 */
public class TestBilling9 {
    CLogger logger = Utils.getLogger(TestBilling9.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig config;
    FlatNetworkServiceSimulatorConfig fconfig;
    KVMSimulatorConfig kconfig;
    PrimaryStorageOverProvisioningManager psRatioMgr;
    HostCapacityOverProvisioningManager hostRatioMgr;
    long totalSize = SizeUnit.GIGABYTE.toByte(100);

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/mevoco/TestMevoco9.xml", con);
        deployer.addSpringConfig("mevocoRelated.xml");
        deployer.addSpringConfig("billing.xml");
        deployer.load();

        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(LocalStorageSimulatorConfig.class);
        fconfig = loader.getComponent(FlatNetworkServiceSimulatorConfig.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        psRatioMgr = loader.getComponent(PrimaryStorageOverProvisioningManager.class);
        hostRatioMgr = loader.getComponent(HostCapacityOverProvisioningManager.class);

        Capacity c = new Capacity();
        c.total = totalSize;
        c.avail = totalSize;

        config.capacityMap.put("host1", c);

        deployer.build();
        api = deployer.getApi();
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        VmInstanceInventory vm = deployer.vms.get("TestVm1");
        VolumeInventory rootVolume = vm.getRootVolume();
        VmInstanceInventory vm2 = deployer.vms.get("TestVm2");

        long during = 5;
        TimeUnit.SECONDS.sleep(during);

        long date1970 = new Date(0).getTime();

        APICreateResourcePriceMsg msg = new APICreateResourcePriceMsg();
        msg.setTimeUnit("s");
        msg.setPrice(100d);
        msg.setResourceName(BillingConstants.SPENDING_CPU);
        msg.setDateInLong(date1970);
        PriceInventory cp = api.createPrice(msg);

        msg = new APICreateResourcePriceMsg();
        msg.setTimeUnit("s");
        msg.setPrice(10d);
        msg.setResourceName(BillingConstants.SPENDING_MEMORY);
        msg.setResourceUnit("m");
        msg.setDateInLong(date1970);
        PriceInventory mp = api.createPrice(msg);

        msg = new APICreateResourcePriceMsg();
        msg.setTimeUnit("s");
        msg.setPrice(9d);
        msg.setResourceName(BillingConstants.SPENDING_ROOT_VOLUME);
        msg.setResourceUnit("m");
        msg.setDateInLong(date1970);
        PriceInventory vp = api.createPrice(msg);

        APICalculateAccountSpendingReply reply = api.calculateSpending(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID, null);

        double cpuPrice = vm.getCpuNum() * 100d * during;
        double memPrice = vm.getMemorySize() * 10d * during;
        long volSizeInM = SizeUnit.BYTE.toMegaByte(rootVolume.getSize());
        double volPrice = volSizeInM * 9d * during;

        // for 2s error margin
        double cpuPriceErrorMargin = vm.getCpuNum() * 100d * 2;
        double memPriceErrorMargin = vm.getMemorySize() * 100d * 2;
        double volPriceErrorMargin = volSizeInM * 9d * 2;
        double errorMargin = cpuPriceErrorMargin + memPriceErrorMargin + volPriceErrorMargin;

        Assert.assertEquals((cpuPrice + memPrice + volPrice) * 2, reply.getTotal(), errorMargin);

        Spending spending = CollectionUtils.find(reply.getSpending(), arg -> BillingConstants.SPENDING_TYPE_VM.equals(arg.getSpendingType()) ? arg : null);
        Assert.assertNotNull(spending);
        List<VmSpending> vmSpendings = JSONObjectUtil.toCollection(JSONObjectUtil.toJsonString(spending.getDetails()),
                ArrayList.class, VmSpending.class);

        Assert.assertEquals(2, vmSpendings.size());

        VmSpending vmSpending = vmSpendings.stream().filter(s -> vm.getUuid().equals(s.resourceUuid)).findFirst().get();
        Assert.assertNotNull(vmSpending);

        double cpuSpending = (double) vmSpending.cpuInventory.stream().mapToDouble(i -> i.spending).sum();
        Assert.assertEquals(cpuPrice, cpuSpending, cpuPriceErrorMargin);

        double memSpending = (double) vmSpending.memoryInventory.stream().mapToDouble(i -> i.spending).sum();
        Assert.assertEquals(memPrice, memSpending, memPriceErrorMargin);

        spending = CollectionUtils.find(reply.getSpending(), arg -> BillingConstants.SPENDING_TYPE_ROOT_VOLUME.equals(arg.getSpendingType()) ? arg : null);
        Assert.assertNotNull(spending);
        Assert.assertEquals(volPrice * 2, spending.getSpending(), volPriceErrorMargin);
        RootVolumeSpending rootVolumeSpending = (RootVolumeSpending) spending.getDetails().stream()
                .filter(d -> d.resourceUuid.equals(vm.getRootVolumeUuid())).findAny().get();

        double rootVolSpending = (double) rootVolumeSpending.sizeInventory.stream().mapToDouble(i -> i.spending).sum();
        Assert.assertEquals(volPrice, rootVolSpending, volPriceErrorMargin);

        vmSpending = vmSpendings.stream().filter(s -> vm2.getUuid().equals(s.resourceUuid)).findFirst().get();
        Assert.assertNotNull(vmSpending);

        cpuSpending = (double) vmSpending.cpuInventory.stream().mapToDouble(i -> i.spending).sum();
        Assert.assertEquals(cpuPrice, cpuSpending, cpuPriceErrorMargin);

        memSpending = (double) vmSpending.memoryInventory.stream().mapToDouble(i -> i.spending).sum();
        Assert.assertEquals(memPrice, memSpending, memPriceErrorMargin);

        spending = CollectionUtils.find(reply.getSpending(), arg -> BillingConstants.SPENDING_TYPE_ROOT_VOLUME.equals(arg.getSpendingType()) ? arg : null);
        Assert.assertNotNull(spending);
        rootVolumeSpending = (RootVolumeSpending) spending.getDetails().stream()
                .filter(d -> d.resourceUuid.equals(vm2.getRootVolumeUuid())).findAny().get();

        rootVolSpending = (double) rootVolumeSpending.sizeInventory.stream().mapToDouble(i -> i.spending).sum();
        Assert.assertEquals(volPrice, rootVolSpending, volPriceErrorMargin);

        api.deletePrice(cp.getUuid());
        api.deletePrice(vp.getUuid());
        api.deletePrice(mp.getUuid());

        reply = api.calculateSpending(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID, null);
        Assert.assertEquals(0, reply.getTotal(), 0.02);
    }
}
