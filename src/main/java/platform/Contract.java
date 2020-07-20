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
        ARG_NUM_WRONG("Incorrect number of arguments '%s'");


        private String tmpl;

        private Message(String tmpl) {
            this.tmpl = tmpl;
        }

        public String template() {
            return this.tmpl;
        }

    }

    @Transaction(name = "InitCredit", intent = Transaction.TYPE.SUBMIT)
    public void initCredit(final Context ctx, final String userID){
        ChaincodeStub stub = ctx.getStub();
        stub.putStringState(userID+"-Credit", "100");
    }

    @Transaction(name = "ChangeCredit", intent = Transaction.TYPE.SUBMIT)
    public void changeCredit(final Context ctx, final String userID, final String changeValue){
        ChaincodeStub stub = ctx.getStub();
        String oldCredit = stub.getStringState(userID+"-Credit");
        try {
            Integer.valueOf(changeValue);
        }catch (Exception e){
            String errorMessage = String.format(Message.ARG_NUM_WRONG.template(),changeValue);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage,e);
        }

        int newCredit = Integer.parseInt(oldCredit) + Integer.parseInt(changeValue);
        if (newCredit < 0){
            String errorMessage = String.format(Message.ARG_NUM_WRONG.template(),changeValue);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage);
        }

        stub.putStringState(userID+"-Credit",String.valueOf(newCredit));

    }
}
