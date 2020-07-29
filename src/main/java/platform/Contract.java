package platform;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.*;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;

import java.util.HashMap;
import java.util.Map;

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
public final class Contract implements ContractInterface {
    enum Message {
        ARG_NUM_WRONG("Incorrect number of arguments '%s'"),
        USER_NOT_EXISTING("User '%s' does not exist."),
        RULE_NOT_EXIST("rule '%s' not exist"),
        GROUP_BUYING_NOT_EXIST("this group buying order '%s' not exist"),
        GROUP_BUYING_NOT_SUCCESS("this group buying order '%s' not success"),
        Transaction_ERROR("Group buying order '%s' not belong to rule '%s'"),
        Transaction_NOT_EXIST("this transaction '%s' not exist");
        private String tmpl;

        private Message(String tmpl) {
            this.tmpl = tmpl;
        }

        public String template() {
            return this.tmpl;
        }

    }

    @Transaction(name = "InitCredit", intent = Transaction.TYPE.SUBMIT)
    public String initCredit(final Context ctx, final String userID) {
        ChaincodeStub stub = ctx.getStub();
        stub.putStringState(userID + "-Credit", "100");
        return "ok";
    }

    @Transaction(name = "ChangeCredit", intent = Transaction.TYPE.SUBMIT)
    public String changeCredit(final Context ctx, final String userID, final String changeValue) {
        ChaincodeStub stub = ctx.getStub();
        String oldCredit = stub.getStringState(userID + "-Credit");
        try {
            Integer.valueOf(changeValue);
        } catch (Exception e) {
            String errorMessage = String.format(Message.ARG_NUM_WRONG.template(), changeValue);
            return errorMessage;
        }

        int newCredit = Integer.parseInt(oldCredit) + Integer.parseInt(changeValue);
        if (newCredit < 0) {
            stub.putStringState(userID + "-Credit", String.valueOf(0));
        }

        stub.putStringState(userID + "-Credit", String.valueOf(newCredit));
        return "ok";

    }

    @Transaction(name = "InitTrans", intent = Transaction.TYPE.SUBMIT)
    public String initTrans(final Context ctx, final String discountRuleID, final String groupBuyingID) {
        ChaincodeStub stub = ctx.getStub();
        String discountRuleString = stub.getStringState(discountRuleID);
        if (discountRuleString.isEmpty()) {
            String errorMessage = String.format(Message.RULE_NOT_EXIST.template(), discountRuleID);
            return errorMessage;
        }
        JSONObject discountRule = JSONObject.parseObject(discountRuleString);

        String groupBuyingString = stub.getStringState(groupBuyingID);
        if (groupBuyingString.isEmpty()) {
            String errorMessage = String.format(Message.GROUP_BUYING_NOT_EXIST.template(), groupBuyingID);
            return errorMessage;
        }
        JSONObject groupBuying = JSONObject.parseObject(groupBuyingString);

        if (groupBuying.getString("currentNum").equals(groupBuying.getString("groupNum"))) {
            if(groupBuying.getString("discountRuleID").equals(discountRuleID)){
                discountRule.put("orderNum", "0");
                discountRule.put("orderIDs", "");
                String payerIDs = groupBuying.getString("userID");
                String payments = discountRule.getString("firstBuyerPrice");
                int receivables = Integer.parseInt(discountRule.getString("firstBuyerPrice"));
                for(int i =2;i<=Integer.parseInt(groupBuying.getString("groupNum"));i++){
                    String participateBuyingString = stub.getStringState(groupBuyingID+"-"+String.valueOf(i));
                    JSONObject participateBuying = JSONObject.parseObject(participateBuyingString);
                    payerIDs += "/" + participateBuying.getString("userID");
                    payments += "/" + discountRule.getString("otherBuyerPrice");
                    receivables += Integer.parseInt(discountRule.getString("otherBuyerPrice"));
                }
                String sellerIDs = discountRule.getString("sellerID");
                Map<String,String> map = new HashMap<>();
                map.put("transState","0");   //0代表支付待完成 1代表支付已完成 -1代表违约
                map.put("payerIDs", payerIDs);
                map.put("payee",sellerIDs);
                map.put("payments",payments);
                map.put("receivables",String.valueOf(receivables));
                //创建交易单
                stub.putStringState(groupBuyingID+"-"+discountRuleID, JSON.toJSONString(map));
                return "ok";

            }else{
                String errorMessage = String.format(Message.Transaction_ERROR.template(), groupBuyingID, discountRuleID);
                return errorMessage;
            }
        }else{
            String errorMessage = String.format(Message.GROUP_BUYING_NOT_SUCCESS.template(), groupBuyingID);
            return errorMessage;
        }


    }

    @Transaction(name = "ChangeTrans", intent = Transaction.TYPE.SUBMIT)
    public String changeTrans(final Context ctx, final String transID, final String transState) {
        ChaincodeStub stub = ctx.getStub();
        String transactionString = stub.getStringState(transID);
        if (transactionString.isEmpty()) {
            String errorMessage = String.format(Message.Transaction_NOT_EXIST.template(), transID);
            return errorMessage;
        }
        JSONObject transaction = JSONObject.parseObject(transactionString);
        transaction.put("transState", transState);
        stub.putStringState(transID,JSON.toJSONString(transaction));
        return "ok";
    }

    @Transaction(name = "QueryTrans", intent = Transaction.TYPE.EVALUATE)
    public String queryTrans(final Context ctx, final String transID) {
        ChaincodeStub stub = ctx.getStub();
        String transactionString = stub.getStringState(transID);
        if (transactionString.isEmpty()) {
            String errorMessage = String.format(Message.Transaction_NOT_EXIST.template(), transID);
            return errorMessage;
        }
        JSONObject transaction = JSONObject.parseObject(transactionString);
        String state = "";
        switch(transaction.getString("transState")){
            case "0": state="支付待完成";break;
            case "1": state="支付已完成";break;
            case "-1": state="用户已违约";break;
        }
        return "交易" + transID + "的交易状态为：" + state;
    }

    @Transaction(name = "QueryCredit", intent = Transaction.TYPE.EVALUATE)
    public String queryCredit(final Context ctx, final String userID) {
        ChaincodeStub stub = ctx.getStub();
        String value = stub.getStringState(userID + "-Credit");
        if (value.isEmpty()) {
            String errorMessage = String.format(Message.USER_NOT_EXISTING.template(), userID);
            return errorMessage;
        }
        return "用户" + userID + "的信用分为：" + value;
    }
}