/**
 * Created by jinyongsuk on 10/8/15.
 */
public class NotEqualOp extends ComparisonOp {
    // Name of the Type (e.g., int, bool, some structdef, etc.)
    private String m_OpName;
    //private int m_size;

    //----------------------------------------------------------------
    //
    //----------------------------------------------------------------
    public NotEqualOp(String strName )
    {
        super(strName);
        setName(strName);
        //setSize(size);
    }

    STO checkOperands(STO a, STO b) {


        if (a.getType().isNumeric() && b.getType().isNumeric()) {
            //System.out.println("Inside Not Equal Op");
            return new ExprSTO(a.getName() + b.getName(), new BoolType("bool", 4));
        } else if (a.getType().isBool() && b.getType().isBool()){
            return new ExprSTO(a.getName() + b.getName(), new BoolType("bool", 4));
        }
        else
        {
            //if it's not both integer then return error STO
            // should increment m_nNumErrors++; in MyParser
            if (a.getType().isNumeric())
                return new ErrorSTO(Formatter.toString(ErrorMsg.error1w_Expr, b.getType().getName(),"!=",a.getType().getName()));
            else
                return new ErrorSTO(Formatter.toString(ErrorMsg.error1w_Expr, a.getType().getName(),"!=",b.getType().getName()));
        }
    }
    //----------------------------------------------------------------
    //
    //----------------------------------------------------------------
    public String getName()
    {
        return m_OpName;
    }

    //----------------------------------------------------------------
    //
    //----------------------------------------------------------------
    private void setName(String str)
    {
        m_OpName = str;
    }
}
