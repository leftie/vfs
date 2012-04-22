package vfs;

import junit.framework.TestSuite;
import vfs.impl.core.BBByteSinkAndSrcTest;
import vfs.impl.core.BlockDeviceTest;
import vfs.impl.core.BlockTest;
import vfs.impl.core.NaiveAllocTest;
import vfs.impl.proto.NodeFlagsTest;
import vfs.impl.proto.ProtoVFSTest;

public class VFSTestSuite extends TestSuite{

        public VFSTestSuite(final String name) {
            super(name);
        }

        public static TestSuite suite() {

            final TestSuite suite = new VFSTestSuite("VFSTestSuite");

            suite.addTestSuite(NaiveAllocTest.class);
            suite.addTestSuite(BlockTest.class);
            suite.addTestSuite(BlockDeviceTest.class);
            suite.addTestSuite(BBByteSinkAndSrcTest.class);
            suite.addTestSuite(NodeFlagsTest.class);
            suite.addTestSuite(ProtoVFSTest.class);

            return suite;
        }
}
