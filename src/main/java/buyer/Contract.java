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
        RULE_STATE_ERROR("rule '%s' not valid"),
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

    /**
     * Initialize Group Buying
     * @param ctx context
     */
    @Transaction(name = "BuyerInit", intent = Transaction.TYPE.SUBMIT)
    public void init(final Context ctx, final String userID,
                     final String sellerID,final String groupBuyingID,
                     final String goodID, final String discountRuleID){
        ChaincodeStub stub = ctx.getStub();
        //获取优惠规则
        String ruleString = stub.getStringState(discountRuleID);
        if (ruleString.isEmpty()){
            String errorMessage = String.format(Message.RULE_NOT_EXIST.template(), discountRuleID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage);
        }
        JSONObject discountRule =  JSONObject.parseObject(ruleString);
        String orderIDs = discountRule.getString("orderIDs");
        if(orderIDs == ""){
            discountRule.put("orderIDs", groupBuyingID);
        }else{
            discountRule.put("orderIDs", orderIDs + "," +groupBuyingID);
        }
        String orderNum = discountRule.getString("orderNum");
        discountRule.put("orderNum", orderNum+1);
        //判断优惠规则状态
        if (Integer.parseInt(discountRule.getString("ruleState")) == 0){
            String errorMessage = String.format(Message.RULE_STATE_ERROR.template(), discountRuleID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage);
        }else {
            int initTime = (int) System.currentTimeMillis();
            //判断优惠规则时限
            if (initTime > Integer.parseInt(discountRule.getString("endTime"))){
                String errorMessage = String.format(Message.RULE_STATE_ERROR.template(), discountRuleID);
                System.out.println(errorMessage);
                throw new ChaincodeException(errorMessage);
            }
            Map<String,String> map = new HashMap<>();
            map.put("userID",userID);
            map.put("sellerID",sellerID);
            map.put("groupBuyingID",groupBuyingID);
            map.put("initTime",String.valueOf(initTime));
            map.put("groupNum",discountRule.getString("groupNum"));
            map.put("goodID",goodID);
            map.put("currentNum", "1");
            map.put("discountRuleID",discountRuleID);
            //初始化（新建拼单）
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
        //获取拼单信息
        String groupBuyingString = stub.getStringState(groupBuyingID);
        if (groupBuyingString.isEmpty()){
            String errorMessage = String.format(Message.GROUP_BUYING_NOT_EXIST.template(), groupBuyingID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage);
        }
        JSONObject groupBuying =  JSONObject.parseObject(groupBuyingString);
        String participateTime = String.valueOf(System.currentTimeMillis());
        //判断拼团时间
        JSONObject discountRule =  JSONObject.parseObject(stub.getStringState(groupBuying.getString("discountRuleID")));
        if (Integer.parseInt(participateTime) > Integer.parseInt(discountRule.getString("endTime"))){
            String errorMessage = String.format(Message.RULE_STATE_ERROR.template(), groupBuying.getString("discountRuleID"));
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage);
        }
        String userNum = String.valueOf(Integer.parseInt(groupBuying.getString("currentNum")) + 1);
        //判断人数是否超限
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
        //参加拼团
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
        //查询当前拼团的状态
        String groupBuyingString = stub.getStringState(groupBuyingID);
        if (groupBuyingString.isEmpty()){
            String errorMessage = String.format(Message.GROUP_BUYING_NOT_EXIST.template(), groupBuyingID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage);
        }
        JSONObject groupBuying =  JSONObject.parseObject(groupBuyingString);
        String groupNum = groupBuying.getString("groupNum");
        String currentNum = groupBuying.getString("currentNum");
        return "成团人数:" + groupNum + ", 当前人数:" + currentNum;
    }
}
