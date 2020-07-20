package buyer;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.*;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;

import java.util.HashMap;
import java.util.Map;
import com.alibaba.fastjson.*;

/**
 * Class: Contract
 */
@org.hyperledger.fabric.contract.annotation.Contract(
        name = "buyer.Contract",
        info = @Info(
                title = "Contract",
                description = "SmartContract for buyer",
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
        NUM_EXCEED("num exceed"),
        RULE_STATE_ERROR("rule not valid");

        private String tmpl;

        private Message(String tmpl) {
            this.tmpl = tmpl;
        }

        public String template() {
            return this.tmpl;
        }
    }

    /**
     * Initialize Group Buying
     * @param ctx context
     */
    @Transaction(name = "Init", intent = Transaction.TYPE.SUBMIT)
    public void init(final Context ctx, final String userID,
                     final String sellerID,final String groupBuyingID,
                     final String goodID, final String discountRuleID){
        ChaincodeStub stub = ctx.getStub();
        JSONObject discountRule =  JSONObject.parseObject(stub.getStringState(discountRuleID));
        if (Integer.parseInt(discountRule.getString("ruleState")) == 0){
            System.out.println(Message.RULE_STATE_ERROR.template());
            throw new ChaincodeException(Message.RULE_STATE_ERROR.template());
        }else {
            int initTime = (int) System.currentTimeMillis();
            if (initTime > Integer.parseInt(discountRule.getString("endTime"))){
                System.out.println(Message.RULE_STATE_ERROR.template());
                throw new ChaincodeException(Message.RULE_STATE_ERROR.template());
            }
            Map<String,String> map = new HashMap<>();
            map.put("userID",userID);
            map.put("sellerID",sellerID);
            map.put("groupBuyingID",groupBuyingID);
            map.put("initTime",String.valueOf(initTime));
            map.put("groupNum",discountRule.getString("groupNum"));
            map.put("goodID",goodID);
            map.put("currentNum", "1");
            stub.putStringState(groupBuyingID,JSON.toJSONString(map));
        }

    }

    /**
     * Participate Group Buying
     * @param ctx
     * @param userID
     * @param groupBuyingID
     */
    @Transaction(name = "Participate", intent = Transaction.TYPE.SUBMIT)
    public void participate(final Context ctx, final String userID,
                            final String groupBuyingID){
        ChaincodeStub stub = ctx.getStub();
        String participateTime = String.valueOf(System.currentTimeMillis());
        JSONObject groupBuying =  JSONObject.parseObject(stub.getStringState(groupBuyingID));
        String userNum = String.valueOf(Integer.parseInt(groupBuying.getString("currentNum")) + 1);
        if (Integer.parseInt(userNum) > Integer.parseInt(groupBuying.getString("groupNum"))){
            System.out.println(Message.NUM_EXCEED.template());
            throw new ChaincodeException(Message.NUM_EXCEED.template());
        }
        groupBuying.put("currentNum",userNum);
        stub.putStringState(groupBuyingID,groupBuying.toJSONString());
        Map<String,String> map = new HashMap<>();
        map.put("userID", userID);
        map.put("participateTime", participateTime);
        map.put("groupBuyingID",groupBuyingID);
        stub.putStringState(groupBuyingID+"-"+userNum,JSON.toJSONString(map));
    }

    /**
     * Query Group Buying State
     * @param ctx
     * @param groupBuyingID
     * @return
     */
    @Transaction(name = "QueryGroupBuying", intent = Transaction.TYPE.EVALUATE)
    public String queryGroupBuying(final Context ctx, final String groupBuyingID){
        ChaincodeStub stub = ctx.getStub();
        JSONObject groupBuying =  JSONObject.parseObject(stub.getStringState(groupBuyingID));
        String groupNum = groupBuying.getString("groupNum");
        String currentNum = groupBuying.getString("currentNum");
        return "成团人数:" + groupNum + ", 当前人数:" + currentNum;
    }
}
