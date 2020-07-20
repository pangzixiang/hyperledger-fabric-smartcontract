package seller;


import org.hyperledger.fabric.contract.Context;    //引入事务上下文类，维护与交易逻辑相关的合同和交易信息
import org.hyperledger.fabric.contract.ContractInterface;     //合约处理接口类,用来响应接收的事务处理
import org.hyperledger.fabric.contract.annotation.*;//定义java注解的包
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;    //ChaincodeStub构建状态数据库，用来访问和修改账本，且在链码之间调用

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.alibaba.fastjson.*;

/**
 * Class: Contract
 */
//使用类级别的@Contract注解，定义合约名称和相关信息
@Contract(
    name = "seller.Contract",
    info = @Info(       //使用@Info注释，进一步定义合约具体信息：标题、描述、版本、证书信息、许可证、联系人
        title = "Contract",
        description = "SmartContract for seller",
        version = "1.0.0",
        license = @License(    //使用@License注释，进一步定义许可证信息：名称、url
            name = "Apache 2.0 License",
            url = "http://www.apache.org/licenses/LICENSE-2.0.html"),
        contact = @Contact(   //使用@License注释，进一步定义联系人信息：邮箱、姓名
            email = "313227220@qq.com",
            name = "Group3"
        )
    )
)



@Default
public final class Contract implements ContractInterface {
    enum Message {
        DISCOUNTRULE_NOT_EXISTING("Discount rule '%s' does not exist."),
        RULE_STATE_ERROR("It is already in this state! The state of this rule is %s now.")

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
     * @param ctx context
     */
    @Transaction(name = "Init", intent = Transaction.TYPE.SUBMIT)
    public void init(final Context ctx, final String sellerID, final String discountRuleID, final String goodID, final String groupNum, final String firstBuyerPrice, final String otherBuyerPrice) {
        ChaincodeStub stub = ctx.getStub();
        Map<String,String> map = new HashMap<>();
        map.put("sellerID",sellerID);
        map.put("goodID",goodID);
        map.put("groupNum",groupNum);
        map.put("firstBuyerPrice",firstBuyerPrice);
        map.put("otherBuyerPrice",otherBuyerPrice);
        map.put("ruleState","0");   //"0"为关闭状态 "1"为开放状态  初始化状态为0
        map.put("duration","0");   //初始化规则时长为0,单位为毫秒，下同
        map.put("startTime","0");  //初始化规则时长为0
        map.put("endTime","0");    //初始化规则时长为0
        map.put("orderNum","0");
        map.put("orderIDs","");
        //初始化（新建优惠规则）
        stub.putStringState(discountRuleID,JSON.toJSONString(map));
    }

    /**
     * Open Rule
     * @param ctx context
     * @param discountRuleID
     * @param duration
     */
    @Transaction(name = "Open", intent = Transaction.TYPE.SUBMIT)
    public String query(final Context ctx, final String discountRuleID, final String duration) {
        ChaincodeStub stub = ctx.getStub();
        JSONObject discountRule = JSONObject.parseObject(stub.getStringState(discountRuleID));
        discountRule.put("duration",duration*3600);    //用户输入时长单位为分钟
        Long startTime = System.currentTimeMillis();
        // 优惠规则不存在
        if (discountRule.isEmpty()) {
            String errorMessage = String.format(Message.DISCOUNTRULE_NOT_EXISTING.template(), discountRuleID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage);
        }
        //查询当前优惠规则的状态
        String ruleState = discountRule.get("ruleState");
        if (Interger.parseInt(ruleState)==1) {
            String errorMessage = String.format(Message.RULE_STATE_ERROR.template(), ruleState);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage);
        }else{
            String endTime = String.valueOf(startTime + Long.valueOf(duration));
            discountRule.put("startTime",startTime);
            discountRule.put("endTime",endTime);
            discountRule.put("ruleState","1");
        }
    }

    /**
     * Close Rule
     * @param ctx context
     * @param discountRuleID
     */
    @Transaction(name = "Close", intent = Transaction.TYPE.SUBMIT)
    public String query(final Context ctx, final String discountRuleID) {
        ChaincodeStub stub = ctx.getStub();
        JSONObject discountRule = JSONObject.parseObject(stub.getStringState(discountRuleID));

        // 优惠规则不存在
        if (discountRule.isEmpty()) {
            String errorMessage = String.format(Message.DISCOUNTRULE_NOT_EXISTING.template(), discountRuleID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage);
        }
        //查询当前优惠规则的状态
        String ruleState = discountRule.get("ruleState");
        if (Interger.parseInt(ruleState)==0) {
            String errorMessage = String.format(Message.RULE_STATE_ERROR.template(), ruleState);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage);
        }else {
            discountRule.put("duration", "0");
            discountRule.put("startTime", "0");
            discountRule.put("endTime", "0");
            discountRule.put("ruleState", "0");
        }
    }

    /**
     * Query Participation Information
     * @param ctx context
     * @param discountRuleID
     * @return orderNum and orderIDs
     */
    @Transaction(name = "QueryParticipation", intent = Transaction.TYPE.EVALUATE)
    public String query(final Context ctx, final String discountRuleID) {
        ChaincodeStub stub = ctx.getStub();
        JSONObject discountRule =  JSONObject.parseObject(stub.getStringState(discountRule));

        // 优惠规则不存在
        if (discountRule.isEmpty()) {
            String errorMessage = String.format(Message.DISCOUNTRULE_NOT_EXISTING.template(), discountRuleID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage);
        }

        String orderNum = (String) discountRule.get("orderNum");
        String orderIDs = (String) discountRule.get("orderIDs");
        if(Interger.parseInt(orderNum)==0){
            return "目前没有拼单参与。" ;
        }else{
            return "成功拼单数量：" + orderNum + ", \n成功拼单号分别为: " + orderIDs;
        }

    }

    /**
     * Query State
     * @param ctx context
     * @param discountRuleID
     * @return ruleState and Residual time
     */
    @Transaction(name = "QueryState", intent = Transaction.TYPE.EVALUATE)
    public void transfer(final Context ctx, final String discountRuleID) {
        ChaincodeStub stub = ctx.getStub();
        JSONObject discountRule =  JSONObject.parseObject(stub.getStringState(discountRule));

        // 优惠规则不存在
        if (discountRule.isEmpty()) {
            String errorMessage = String.format(Message.DISCOUNTRULE_NOT_EXISTING.template(), discountRuleID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage);
        }

        String ruleState = (String) discountRule.get("ruleState");
        if (Interger.parseInt(ruleState) == 0){
            return "优惠规则" + discountRuleID + "目前的状态为关闭。"
        }else{
            long currentTime = System.currentTimeMillis();
            long endTime = Long.valueOf(discountRule.get("endTime"));
            return "优惠规则：" + discountRuleID + "目前的状态仍为开放，\n并且将在" + (endTime-currentTime)/3600 + "分" + (endTime-currentTime)%3600 +"秒后关闭";
        }
    }
