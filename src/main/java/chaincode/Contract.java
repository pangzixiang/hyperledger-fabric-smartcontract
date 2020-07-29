package chaincode;
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
                description = "SmartContract",
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
        RULE_NOT_EXIST("rule '%s' not exist"),
        GROUP_BUYING_NOT_EXIST("this group buying order '%s' not exist"),
        DISCOUNTRULE_NOT_EXISTING("Discount rule '%s' does not exist."),
        RULE_STATE_ERROR("This '%s' rule state is error now."),
        RULE_TIMEOUT("This '%s' rule is timeout"),
        ARG_NUM_WRONG("Incorrect number of arguments '%s'"),
        USER_NOT_EXISTING("User '%s' does not exist."),
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

    @Transaction(name = "InitChainCode", intent = Transaction.TYPE.SUBMIT)
    public void initChainCode(final Context ctx) {
        ChaincodeStub stub = ctx.getStub();
        stub.putStringState("ID", "abcd");
    }

    /**
    buyer
     */

    /**
     * Initialize Group Buying
     * @param ctx context
     */
    @Transaction(name = "InitGroup", intent = Transaction.TYPE.SUBMIT)
    public String initGroup(final Context ctx, final String userID,
                     final String groupBuyingID,final String discountRuleID){
        ChaincodeStub stub = ctx.getStub();
        //获取优惠规则
        String ruleString = stub.getStringState(discountRuleID);
        if (ruleString.isEmpty()){
            String errorMessage = String.format(Message.RULE_NOT_EXIST.template(), discountRuleID);
            return errorMessage;
        }
        JSONObject discountRule =  JSONObject.parseObject(ruleString);
        String orderIDs = discountRule.getString("orderIDs");
        if(orderIDs == ""){
            discountRule.put("orderIDs", groupBuyingID);
        }else{
            discountRule.put("orderIDs", orderIDs + "-" +groupBuyingID);
        }
        String orderNum = discountRule.getString("orderNum");
        discountRule.put("orderNum", String.valueOf(Integer.parseInt(orderNum)+1));
        //判断优惠规则状态
        if (Integer.parseInt(discountRule.getString("ruleState")) == 0){
            String errorMessage = String.format(Message.RULE_STATE_ERROR.template(), discountRuleID);
            return errorMessage;
        }else {
            Long initTime = System.currentTimeMillis();
            //判断优惠规则时限
            if (initTime > Long.valueOf(discountRule.getString("endTime"))){
                String errorMessage = String.format(Message.RULE_TIMEOUT.template(), discountRuleID);
                return errorMessage;
            }
            Map<String,String> map = new HashMap<>();
            map.put("userID",userID);
            map.put("sellerID",discountRule.getString("sellerID"));
            map.put("groupBuyingID",groupBuyingID);
            map.put("initTime",String.valueOf(initTime));
            map.put("groupNum",discountRule.getString("groupNum"));
            map.put("goodID",discountRule.getString("goodID"));
            map.put("currentNum", "1");
            map.put("discountRuleID",discountRuleID);
            //初始化（新建拼单）
            stub.putStringState(groupBuyingID,JSON.toJSONString(map));
            return "ok";
        }

    }

    /**
     * Participate Group Buying
     * @param ctx
     * @param userID
     * @param groupBuyingID
     */
    @Transaction(name = "Participate", intent = Transaction.TYPE.SUBMIT)
    public String participate(final Context ctx, final String userID,
                            final String groupBuyingID){
        ChaincodeStub stub = ctx.getStub();
        //获取拼单信息
        String groupBuyingString = stub.getStringState(groupBuyingID);
        if (groupBuyingString.isEmpty()){
            String errorMessage = String.format(Message.GROUP_BUYING_NOT_EXIST.template(), groupBuyingID);
            return errorMessage;
        }
        JSONObject groupBuying =  JSONObject.parseObject(groupBuyingString);
        Long participateTime = System.currentTimeMillis();
        //判断拼团时间
        JSONObject discountRule =  JSONObject.parseObject(stub.getStringState(groupBuying.getString("discountRuleID")));
        if (participateTime > Long.valueOf(discountRule.getString("endTime"))){
            String errorMessage = String.format(Message.RULE_TIMEOUT.template(), groupBuying.getString("discountRuleID"));
            return errorMessage;
        }
        String userNum = String.valueOf(Integer.parseInt(groupBuying.getString("currentNum")) + 1);
        //判断人数是否超限
        if (Integer.parseInt(userNum) > Integer.parseInt(groupBuying.getString("groupNum"))){
            return Message.NUM_EXCEED.template();
        }
        groupBuying.put("currentNum",userNum);
        stub.putStringState(groupBuyingID,groupBuying.toJSONString());
        Map<String,String> map = new HashMap<>();
        map.put("userID", userID);
        map.put("participateTime", String.valueOf(participateTime));
        map.put("groupBuyingID",groupBuyingID);
        //参加拼团
        stub.putStringState(groupBuyingID+"-"+userNum,JSON.toJSONString(map));
        return "ok";
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


    /**
    seller
     */

    /**
     * Initialize Discount Rule
     *
     * @param ctx context
     */
    @Transaction(name = "InitRule", intent = Transaction.TYPE.SUBMIT)
    public String initRule(final Context ctx, final String sellerID, final String discountRuleID, final String goodID, final String groupNum, final String firstBuyerPrice, final String otherBuyerPrice) {
        ChaincodeStub stub = ctx.getStub();
        Map<String, String> map = new HashMap<>();
        map.put("sellerID", sellerID);
        map.put("goodID", goodID);
        map.put("groupNum", groupNum);
        map.put("firstBuyerPrice", firstBuyerPrice);
        map.put("otherBuyerPrice", otherBuyerPrice);
        map.put("ruleState", "0");   //"0"为关闭状态 "1"为开放状态  初始化状态为0
        map.put("duration", "0");   //初始化规则时长为0,单位为毫秒，下同
        map.put("startTime", "0");  //初始化规则时长为0
        map.put("endTime", "0");    //初始化规则时长为0
        map.put("orderNum", "0");
        map.put("orderIDs", "");
        //初始化（新建优惠规则）
        stub.putStringState(discountRuleID, JSON.toJSONString(map));
        return "ok";
    }

    /**
     * Open Rule
     *
     * @param ctx            context
     * @param discountRuleID
     * @param duration
     */
    @Transaction(name = "Open", intent = Transaction.TYPE.SUBMIT)
    public String open(final Context ctx, final String discountRuleID, final String duration) {
        ChaincodeStub stub = ctx.getStub();
        JSONObject discountRule = JSONObject.parseObject(stub.getStringState(discountRuleID));
        discountRule.put("duration", Integer.parseInt(duration) * 3600);    //用户输入时长单位为分钟
        Long startTime = System.currentTimeMillis();
        // 优惠规则不存在
        if (discountRule.isEmpty()) {
            String errorMessage = String.format(Message.DISCOUNTRULE_NOT_EXISTING.template(), discountRuleID);
            return errorMessage;
        }
        //查询当前优惠规则的状态
        String ruleState = discountRule.getString("ruleState");
        if (Integer.parseInt(ruleState) == 1) {
            String errorMessage = String.format(Message.RULE_STATE_ERROR.template(), ruleState);
            return errorMessage;
        } else {
            String endTime = String.valueOf(startTime + Long.valueOf(duration)*3600);
            discountRule.put("startTime", startTime);
            discountRule.put("endTime", endTime);
            discountRule.put("ruleState", "1");
            stub.putStringState(discountRuleID,JSON.toJSONString(discountRule));
            return "ok";
        }
    }

    /**
     * Close Rule
     *
     * @param ctx            context
     * @param discountRuleID
     */
    @Transaction(name = "Close", intent = Transaction.TYPE.SUBMIT)
    public String close(final Context ctx, final String discountRuleID) {
        ChaincodeStub stub = ctx.getStub();
        JSONObject discountRule = JSONObject.parseObject(stub.getStringState(discountRuleID));

        // 优惠规则不存在
        if (discountRule.isEmpty()) {
            String errorMessage = String.format(Message.DISCOUNTRULE_NOT_EXISTING.template(), discountRuleID);
            return errorMessage;
        }
        //查询当前优惠规则的状态
        String ruleState = discountRule.getString("ruleState");
        if (Integer.parseInt(ruleState) == 0) {
            String errorMessage = String.format(Message.RULE_STATE_ERROR.template(), ruleState);
            return errorMessage;
        } else {
            discountRule.put("duration", "0");
            discountRule.put("startTime", "0");
            discountRule.put("endTime", "0");
            discountRule.put("ruleState", "0");
            stub.putStringState(discountRuleID,JSON.toJSONString(discountRule));
            return "ok";
        }
    }

    /**
     * Query Participation Information
     *
     * @param ctx            context
     * @param discountRuleID
     * @return orderNum and orderIDs
     */
    @Transaction(name = "QueryParticipation", intent = Transaction.TYPE.EVALUATE)
    public String queryParticipation(final Context ctx, final String discountRuleID) {
        ChaincodeStub stub = ctx.getStub();
        JSONObject discountRule = JSONObject.parseObject(stub.getStringState(discountRuleID));

        // 优惠规则不存在
        if (discountRule.isEmpty()) {
            String errorMessage = String.format(Message.DISCOUNTRULE_NOT_EXISTING.template(), discountRuleID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage);
        }

        String orderNum = (String) discountRule.get("orderNum");
        String orderIDs = (String) discountRule.get("orderIDs");
        if (Integer.parseInt(orderNum) == 0) {
            return "目前没有拼单参与。";
        } else {
            return "成功拼单数量：" + orderNum + ", \n成功拼单号分别为: " + orderIDs;
        }

    }

    /**
     * Query State
     *
     * @param ctx            context
     * @param discountRuleID
     * @return ruleState and Residual time
     */
    @Transaction(name = "QueryState", intent = Transaction.TYPE.EVALUATE)
    public String queryState(final Context ctx, final String discountRuleID) {
        ChaincodeStub stub = ctx.getStub();
        JSONObject discountRule = JSONObject.parseObject(stub.getStringState(discountRuleID));

        // 优惠规则不存在
        if (discountRule.isEmpty()) {
            String errorMessage = String.format(Message.DISCOUNTRULE_NOT_EXISTING.template(), discountRuleID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage);
        }

        String ruleState = (String) discountRule.get("ruleState");
        if (Integer.parseInt(ruleState) == 0) {
            return "优惠规则" + discountRuleID + "目前的状态为关闭。";
        } else {
            long currentTime = System.currentTimeMillis();
            long endTime = Long.valueOf(discountRule.getString("endTime"));
            return "优惠规则：" + discountRuleID + "目前的状态仍为开放，\n并且将在" + (endTime - currentTime) / 3600 + "分" + (endTime - currentTime) % 3600 + "秒后关闭";
        }
    }


    /**
    platform
     */
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
