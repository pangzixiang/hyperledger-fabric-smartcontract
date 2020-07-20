package seller;


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
    name = "seller.Contract",
    info = @Info(
        title = "Contract",
        description = "SmartContract for seller",
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
        DISCOUNTRULE_NOT_EXISTING("Discount rule '%s' does not exist."),
        RULE_STATE_ERROR("It is already in this state! The state of this rule is %s now.");

        private String tmpl;

        private Message(String tmpl) {
            this.tmpl = tmpl;
        }

        public String template() {
            return this.tmpl;
        }
        }

    /**
     * Initialize Discount Rule
     *
     * @param ctx context
     */
    @Transaction(name = "Init", intent = Transaction.TYPE.SUBMIT)
    public void init(final Context ctx, final String sellerID, final String discountRuleID, final String goodID, final String groupNum, final String firstBuyerPrice, final String otherBuyerPrice) {
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
    }

    /**
     * Open Rule
     *
     * @param ctx            context
     * @param discountRuleID
     * @param duration
     */
    @Transaction(name = "Open", intent = Transaction.TYPE.SUBMIT)
    public void query(final Context ctx, final String discountRuleID, final String duration) {
        ChaincodeStub stub = ctx.getStub();
        JSONObject discountRule = JSONObject.parseObject(stub.getStringState(discountRuleID));
        discountRule.put("duration", Integer.parseInt(duration) * 3600);    //用户输入时长单位为分钟
        Long startTime = System.currentTimeMillis();
        // 优惠规则不存在
        if (discountRule.isEmpty()) {
            String errorMessage = String.format(Message.DISCOUNTRULE_NOT_EXISTING.template(), discountRuleID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage);
        }
        //查询当前优惠规则的状态
        String ruleState = discountRule.getString("ruleState");
        if (Integer.parseInt(ruleState) == 1) {
            String errorMessage = String.format(Message.RULE_STATE_ERROR.template(), ruleState);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage);
        } else {
            String endTime = String.valueOf(startTime + Long.valueOf(duration));
            discountRule.put("startTime", startTime);
            discountRule.put("endTime", endTime);
            discountRule.put("ruleState", "1");
        }
    }

    /**
     * Close Rule
     *
     * @param ctx            context
     * @param discountRuleID
     */
    @Transaction(name = "Close", intent = Transaction.TYPE.SUBMIT)
    public void close(final Context ctx, final String discountRuleID) {
        ChaincodeStub stub = ctx.getStub();
        JSONObject discountRule = JSONObject.parseObject(stub.getStringState(discountRuleID));

        // 优惠规则不存在
        if (discountRule.isEmpty()) {
            String errorMessage = String.format(Message.DISCOUNTRULE_NOT_EXISTING.template(), discountRuleID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage);
        }
        //查询当前优惠规则的状态
        String ruleState = discountRule.getString("ruleState");
        if (Integer.parseInt(ruleState) == 0) {
            String errorMessage = String.format(Message.RULE_STATE_ERROR.template(), ruleState);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage);
        } else {
            discountRule.put("duration", "0");
            discountRule.put("startTime", "0");
            discountRule.put("endTime", "0");
            discountRule.put("ruleState", "0");
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
    public String query(final Context ctx, final String discountRuleID) {
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
    public String transfer(final Context ctx, final String discountRuleID) {
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
}
