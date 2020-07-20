package platform;
import com.alibaba.fastjson.JSONObject;
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
        ARG_NUM_WRONG("Incorrect number of arguments '%s'"),
        RULE_NOT_EXIST("rule '%s' not exist"),
        GROUP_BUYING_NOT_EXIST("this group buying order '%s' not exist");


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
            stub.putStringState(userID+"-Credit",String.valueOf(0));
        }

        stub.putStringState(userID+"-Credit",String.valueOf(newCredit));

    }

    @Transaction(name = "InitTrans", intent = Transaction.TYPE.SUBMIT)
    public void initTrans(final Context ctx, final String discountRuleID, final String groupBuyingID){
        ChaincodeStub stub = ctx.getStub();
        String discountRuleString = stub.getStringState(discountRuleID);
        if (discountRuleString.isEmpty()){
            String errorMessage = String.format(Message.RULE_NOT_EXIST.template(), discountRuleID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage);
        }
        JSONObject discountRule = JSONObject.parseObject(discountRuleString);

        String groupBuyingString = stub.getStringState(groupBuyingID);
        if (groupBuyingString.isEmpty()){
            String errorMessage = String.format(Message.GROUP_BUYING_NOT_EXIST.template(), groupBuyingID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage);
        }
        JSONObject groupBuying = JSONObject.parseObject(groupBuyingString);

        if (groupBuying.getString("currentNum").equals(groupBuying.getString("groupNum"))){
            int oldOrderNum = Integer.parseInt(discountRule.getString("orderNum"));
            String oldOrderIDs = discountRule.getString("orderIDs");
            discountRule.put("orderNum", String.valueOf(oldOrderNum+1));
            discountRule.put("orderIDs", oldOrderIDs+groupBuyingID+"/");
        }

    }

}
