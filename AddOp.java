/**
 * Created by jinyongsuk on 10/8/15.
 */
public class AddOp extends ArithmeticOp {
    // Name of the Type (e.g., int, bool, some structdef, etc.)
    private String m_OpName;
    //private int m_size;

    //----------------------------------------------------------------
    //
    //----------------------------------------------------------------
    public AddOp(String strName )
    {
        super(strName);
        //setSize(size);
    }

    STO checkOperands(STO a, STO b)
    {
        Type aType = a.getType();
        Type bType = b.getType();

        if( !(aType.isNumeric()) || !(bType.isNumeric()))
        {
            STO err = (!(aType.isNumeric())) ? b : a;
            // should increment m_nNumErrors++; in MyParser
            return new ErrorSTO(err.getName());
        }
        else if ( aType.isInt() && bType.isInt()){

            System.out.println(a.getName()+""+b.getName() +" has typ: "+ a.getType().toString());
            return new ExprSTO(a.getName()+""+b.getName(), a.getType());
        }
        else{
            STO c = !(aType.isInt()) ? b : a;
            System.out.println(a.getName()+""+b.getName());
            return new ExprSTO(a.getName()+""+b.getName(), c.getType());
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
