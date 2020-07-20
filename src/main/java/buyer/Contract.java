package buyer;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.*;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
        NUM_EXCEED("当前人数大于设定的拼团人数");
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
    public void init(final Context ctx, final String userID, final String sellerID, final String goodID){
        ChaincodeStub stub = ctx.getStub();
        String groupBuyingID = UUID.randomUUID().toString().replace("-","");
        String initTime = String.valueOf(System.currentTimeMillis());
        Map<String,String> map = new HashMap<>();
        map.put("userID",userID);
        map.put("sellerID",sellerID);
        map.put("groupBuyingID",groupBuyingID);
        map.put("initTime",initTime);
        map.put("groupNum","50"); //需要改,查询优惠规则
        map.put("goodID",goodID);
        map.put("currentNum", "1");
        stub.putStringState(groupBuyingID,JSON.toJSONString(map));
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
        String userNum = String.valueOf(Integer.parseInt((String) groupBuying.get("currentNum")) + 1);
        if (Integer.parseInt(userNum) > Integer.parseInt((String) groupBuying.get("groupNum"))){
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
        String groupNum = (String) groupBuying.get("groupNum");
        String currentNum = (String) groupBuying.get("currentNum");
        return "成团人数：" + groupNum + ", 当前人数: " + currentNum;
    }

    /**
     * Query User Credit
     * @param ctx
     * @param userID
     * @return
     */
    @Transaction(name = "QueryCredit", intent = Transaction.TYPE.EVALUATE)
    public String queryCredit(final Context ctx, final String userID){
        ChaincodeStub stub = ctx.getStub();
        return stub.getStringState(userID+"-Credit");
    }
}
