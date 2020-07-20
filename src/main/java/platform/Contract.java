package platform;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.*;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
/**
 * Class: Contract
 */
@org.hyperledger.fabric.contract.annotation.Contract(
        name = "platform.Contract",
        info = @Info(
                title = "Contract",
                description = "SmartContract for platform",
                version = "1.0.0",
                license = @License(
                        name = "Apache 2.0 License",
                        url = "http://www.apache.org/licenses/LICENSE-2.0.html"),
                contact = @Contact(
                        email = "313227220@qq.com",
                        name = "Group3"
                )
        )
)
@Default
public final class Contract implements ContractInterface{
    enum Message {

    }

    @Transaction(name = "InitCredit", intent = Transaction.TYPE.SUBMIT)
    public void initCredit(final Context ctx, final String userID){
        ChaincodeStub stub = ctx.getStub();
        stub.putStringState(userID+"-Credit", "100");
    }
}
