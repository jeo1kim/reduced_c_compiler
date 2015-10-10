/**
 * Created by jinyongsuk on 10/8/15.
 */
public abstract class ArithmeticOp extends BinaryOp {
    // Name of the Type (e.g., int, bool, some structdef, etc.)
    private String m_OpName;
    //private int m_size;

    //----------------------------------------------------------------
    //
    //----------------------------------------------------------------
    public ArithmeticOp(String strName )
    {
        super(strName);
        setName(strName);
        //setSize(size);
    }

    STO checkOperands(STO a, STO b, String opName)
    {


        if( !(a.getType().isNumeric()) || !(b.getType().isNumeric()))
        {
            // should increment m_nNumErrors++; in MyParser
            if (a.getType().isNumeric())
                return new ErrorSTO(Formatter.toString(ErrorMsg.error1w_Expr, b.getType().getName(),opName,a.getType().getName()));
            else
                return new ErrorSTO(Formatter.toString(ErrorMsg.error1w_Expr, a.getType().getName(), opName, b.getType().getName()));

        }
        else if (a.isConst() && b.isConst())
        {
            return new ConstSTO( a.getName()+b.getName() , b.getType());
        }
        else if ( a.getType().isInt() && b.getType().isInt()){
            //System.out.println(a.getName()+b.getName() +" has typ: "+ a.getType().toString());
            return new ExprSTO(a.getName()+b.getName(), a.getType());
        }

        else{
            STO c = !(a.getType().isInt()) ? b : a;
            //System.out.println("A val: " + a.getName()+" B val: "+b.getName());
            return new ExprSTO(a.getName()+b.getName(), c.getType());
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
