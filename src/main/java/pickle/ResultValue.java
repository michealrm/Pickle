package pickle;

import pickle.exception.InvalidNumberException;
import pickle.exception.InvalidOperationException;
import pickle.exception.ResultValueConversionException;

/**
 * A wrapper for the results of expressions
 *
 * `value` conversions from Pickle type -> Java class
 * Integer	-> Numeric
 * Float	-> Numeric
 * String	-> String
 * Bool		-> Boolean
 *
 * @see pickle.SubClassif for the subclassifications (iDatatype) of the tokens
 */
public class ResultValue
{
	public Classif iPrimClassif;
	public SubClassif iDatatype;
	public Object value;
	public boolean isNumber = false;
	public static String scTerminatingStr;
	public static String leftOpGlobal;
	public static String rightOpGlobal;
	public static String operationGlobal;

	// Constructor
	public ResultValue() {}

	/**
	* Start classifying variables and perform certain operations.
	* <p>
	* @param iType: the datatype of the object
	* @param val: the value of the object
	*/
	public ResultValue(SubClassif iType, Object val) throws Exception
	{
		iDatatype = iType;
		value = val;

		// Check if type is a number type
		if (iDatatype == SubClassif.INTEGER || iDatatype == SubClassif.FLOAT)
		{
			if ( !(val instanceof Numeric) )
			{
				value = new Numeric( val.toString(), iDatatype);
			}
			isNumber = true;
		}
		
		// Check if type is an identifier type
		else if (iDatatype == SubClassif.IDENTIFIER)
		{
			if (val instanceof Numeric)
			{
				iDatatype = ((Numeric) val).type;
			}
			else if (val instanceof String)
			{
				iDatatype = SubClassif.STRING;
			}
			else if (val instanceof Boolean)
			{
				iDatatype = SubClassif.BOOLEAN;
			}
			else
			{
				throw new InvalidNumberException("Invalid datatype detected");
			}
		}
		else
		{
			isNumber = false;
		}
	}

	/**
	 * Start classifying variables and perform certain operations.
	 * <p>
	 * @param iClassif: the classification of the object
	 * @param iType: the datatype of the object
	 * @param val: the value of the object
	 */
	public ResultValue(Classif iClassif, SubClassif iType, Object val) throws Exception
	{
		iPrimClassif = iClassif;
		iDatatype = iType;
		value = val;

		// Check if type is a number type
		if (iDatatype == SubClassif.INTEGER || iDatatype == SubClassif.FLOAT)
		{
			if ( !(val instanceof Numeric) )
			{
				value = new Numeric( val.toString(), iDatatype);
			}
			isNumber = true;
		}

		// Check if type is an identifier type
		else if (iDatatype == SubClassif.IDENTIFIER)
		{
			if (val instanceof Numeric)
			{
				iDatatype = ((Numeric) val).type;
			}
			else if (val instanceof String)
			{
				iDatatype = SubClassif.STRING;
			}
			else if (val instanceof Boolean)
			{
				iDatatype = SubClassif.BOOLEAN;
			}
			else
			{
				throw new InvalidNumberException("Invalid datatype detected");
			}
		}
		else
		{
			isNumber = false;
		}
	}

	/**
	* Begin to execute operation according to operands and whatever operators are given
	* @param rightOperand: a ResultValue that is to the right of the operation param
	* @param operation: a String that begins the execution of a certain operation with
						the rightOperand and/or and another value
	* @return ResultValue
	*/
	public ResultValue executeOperation(ResultValue rightOperand, String operation) throws Exception
	{
		Object result;
		SubClassif resultType = iDatatype;

		rightOpGlobal = rightOperand.value.toString();
		leftOpGlobal = value.toString();
		operationGlobal = operation;
		
		if (isNumber)
		{
			if (rightOperand == null)
			{
				// If it's a unary minus
				if (operation.equals("-"))
				{
					result = ((Numeric) value).unaryMinus().toString();
				}
				else
				{
					throw new InvalidOperationException("Invalid operation detected");
				}
			}
			else if (rightOperand.isNumber)
			{
				switch (operation) {
					case "+":
						result = ((Numeric) value).add((Numeric) rightOperand.value).toString();
						break;
					case "-":
						result = ((Numeric) value).subtract((Numeric) rightOperand.value).toString();
						break;
					case "*":
						result = ((Numeric) value).multiply((Numeric) rightOperand.value).toString();
						break;
					case "/":
						result = ((Numeric) value).divide((Numeric) rightOperand.value).toString();
						break;
					case "^":
						result = ((Numeric) value).power((Numeric) rightOperand.value).toString();
						break;
					case "==":
						if (((Numeric) value).equals((Numeric) rightOperand.value)) {
							result = true;
						} else {
							result = false;
						}
						resultType = SubClassif.BOOLEAN;
						break;
					case "!=":
						if (((Numeric) value).notEqual((Numeric) rightOperand.value)) {
							result = true;
						} else {
							result = false;
						}
						resultType = SubClassif.BOOLEAN;
						break;
					case "<":
						if (((Numeric) value).lessThan((Numeric) rightOperand.value)) {
							result = true;
						} else {
							result = false;
						}
						resultType = SubClassif.BOOLEAN;
						break;
					case "<=":
						if (((Numeric) value).lessThanEqualTo((Numeric) rightOperand.value)) {
							result = true;
						} else {
							result = false;
						}
						resultType = SubClassif.BOOLEAN;
						break;
					case ">":
						if (((Numeric) value).greaterThan((Numeric) rightOperand.value)) {
							result = true;
						} else {
							result = false;
						}
						resultType = SubClassif.BOOLEAN;
						break;
					case ">=":
						if (((Numeric) value).greaterThanEqualTo((Numeric) rightOperand.value)) {
							result = true;
						} else {
							result = false;
						}
						resultType = SubClassif.BOOLEAN;
						break;
					default:
						throw new InvalidOperationException("Invalid operation detected");
				}
			}
			else if (rightOperand.iDatatype == SubClassif.STRING)
			{
				Numeric tempRightOperand = 
					new Numeric(((StringBuilder) rightOperand.value).toString(), rightOperand.iDatatype);

				switch (operation) {
					case "+":
						result = ((Numeric) value).add(tempRightOperand).toString();
						break;
					case "-":
						result = ((Numeric) value).subtract(tempRightOperand).toString();
						break;
					case "*":
						result = ((Numeric) value).multiply(tempRightOperand).toString();
						break;
					case "/":
						result = ((Numeric) value).divide(tempRightOperand).toString();
						break;
					case "^":
						result = ((Numeric) value).power(tempRightOperand).toString();
						break;
					case "==":
						if (((Numeric) value).equals(tempRightOperand)) {
							result = true;
						} else {
							result = false;
						}
						resultType = SubClassif.BOOLEAN;
						break;
					case "!=":
						if (((Numeric) value).notEqual(tempRightOperand)) {
							result = true;
						} else {
							result = false;
						}
						resultType = SubClassif.BOOLEAN;
						break;
					case "<":
						if (((Numeric) value).lessThan(tempRightOperand)) {
							result = true;
						} else {
							result = false;
						}
						resultType = SubClassif.BOOLEAN;
						break;
					case "<=":
						if (((Numeric) value).lessThanEqualTo(tempRightOperand)) {
							result = true;
						} else {
							result = false;
						}
						resultType = SubClassif.BOOLEAN;
						break;
					case ">":
						if (((Numeric) value).greaterThan(tempRightOperand)) {
							result = true;
						} else {
							result = false;
						}
						resultType = SubClassif.BOOLEAN;
						break;
					case ">=":
						if (((Numeric) value).greaterThanEqualTo(tempRightOperand)) {
							result = true;
						} else {
							result = false;
						}
						resultType = SubClassif.BOOLEAN;
						break;
					default:
						throw new InvalidOperationException("Invalid operation detected");
				}
			}
			else
			{
				throw new InvalidOperationException("The operand's datatype is invalid for "
					+ operation + " operations");
			}
		}
		else if (iDatatype == SubClassif.BOOLEAN)
		{
			if (rightOperand == null)
			{
				if (operation.equals("not"))
				{
					if (value.equals(true))
					{
						result = false;
					}
					else if (value.equals(false))
					{
						result = true;
					}
					else
					{
						throw new InvalidOperationException("The operand's datatype is invalid for "
							+ operation + " operations");
					}
				}
				else
				{
					throw new InvalidOperationException("Invalid operation detected");
				}
			}
			else if (rightOperand.iDatatype == SubClassif.BOOLEAN)
			{
				// Begin a boolean operation with the rightOperand and the operation passed in

				switch (operation) {
					case "and":
						if ((Boolean) value && (Boolean) rightOperand.value) {
							result = true;
						} else {
							result = false;
						}
						break;
					case "or":
						if ((Boolean) value || (Boolean) rightOperand.value) {
							result = true;
						} else {
							result = false;
						}
						break;
					case "==":
						if (value.equals(rightOperand.value)) {
							result = true;
						} else {
							result = false;
						}
						break;
					case "!=":
						if (!value.equals(rightOperand.value)) {
							result = true;
						} else {
							result = false;
						}
						break;
					default:
						throw new InvalidOperationException("The operand's datatype is invalid for "
								+ operation + " operations");
				}
			}
			else
			{
				throw new InvalidOperationException("The operand's datatype is invalid for "
					+ operation + " operations");
			}
		}
		else if (iDatatype == SubClassif.STRING)
		{
			String scRightOperand = "";
			
			if (rightOperand.iDatatype == SubClassif.STRING)
			{
				if(rightOperand.value instanceof String)
					scRightOperand = (String)rightOperand.value;
				else
					scRightOperand = ((StringBuilder) rightOperand.value).toString();
			}
			else if (rightOperand.iDatatype == SubClassif.BOOLEAN)
			{
				scRightOperand = ((Boolean) rightOperand.value).toString();
			}
			else if (rightOperand.isNumber)
			{
				scRightOperand = ((Numeric) rightOperand.value).toString();
			}
			else
			{
				throw new InvalidOperationException("The operand's datatype is invalid for "
					+ operation + " operations");
			}

			switch (operation) {
				case "#":
					result = new StringBuilder(valueToString(value).concat(scRightOperand));
					break;
				case "+": {
					Numeric leftOp = new Numeric(value.toString(), SubClassif.STRING);
					Numeric rightOp = new Numeric(rightOperand.value.toString(), SubClassif.STRING);

					result = leftOp.add(rightOp);
					resultType = leftOp.type;
					break;
				}
				case "-": {
					Numeric leftOp = new Numeric(value.toString(), SubClassif.STRING);
					Numeric rightOp = new Numeric(rightOperand.value.toString(), SubClassif.STRING);

					result = leftOp.subtract(rightOp);
					resultType = leftOp.type;
					break;
				}
				case "*": {
					Numeric leftOp = new Numeric(value.toString(), SubClassif.STRING);
					Numeric rightOp = new Numeric(rightOperand.value.toString(), SubClassif.STRING);

					result = leftOp.multiply(rightOp);
					resultType = leftOp.type;
					break;
				}
				case "/": {
					Numeric leftOp = new Numeric(value.toString(), SubClassif.STRING);
					Numeric rightOp = new Numeric(rightOperand.value.toString(), SubClassif.STRING);

					result = leftOp.divide(rightOp);
					resultType = leftOp.type;
					break;
				}
				case "^": {
					Numeric leftOp = new Numeric(value.toString(), SubClassif.STRING);
					Numeric rightOp = new Numeric(rightOperand.value.toString(), SubClassif.STRING);

					result = leftOp.power(rightOp);
					resultType = leftOp.type;
					break;
				}
				case "==":
					String valueStr = valueToString(value);
					if (valueStr.equals(scRightOperand)) {
						result = true;
					} else {
						result = false;
					}
					resultType = SubClassif.BOOLEAN;
					break;
				case "!=":
					if (valueToString(value).compareTo(scRightOperand) != 0) {
						result = true;
					} else {
						result = false;
					}
					resultType = SubClassif.BOOLEAN;
					break;
				case "<":
					if (valueToString(value).compareTo(scRightOperand) < 0) {
						result = true;
					} else {
						result = false;
					}
					resultType = SubClassif.BOOLEAN;
					break;
				case "<=":
					if (valueToString(value).compareTo(scRightOperand) <= 0) {
						result = true;
					} else {
						result = false;
					}
					resultType = SubClassif.BOOLEAN;
					break;
				case ">":
					if (valueToString(value).compareTo(scRightOperand) > 0) {
						result = true;
					} else {
						result = false;
					}
					resultType = SubClassif.BOOLEAN;
					break;
				case ">=":
					if (valueToString(value).compareTo(scRightOperand) >= 0) {
						result = true;
					} else {
						result = false;
					}
					resultType = SubClassif.BOOLEAN;
					break;
				default:
					throw new InvalidOperationException("Invalid operation detected");
			}
		}
		else
		{
			throw new InvalidOperationException("The operand's datatype is invalid for "
				+ operation + " operations");
		}
		return new ResultValue(resultType, result);
	}



	/**
	 * Convert given value to a certain data type
	 * @param iTargetType
	 * @param valueToConvert
	 * @return
	 * @throws Exception
	 */
	public static ResultValue convertType(SubClassif iTargetType, ResultValue valueToConvert) throws Exception
	{
		ResultValue returnVal = null;

		if (valueToConvert == null)
		{
			throw new NullPointerException("Tried to convert a null ResultValue object");
		}
		else if (valueToConvert.value == null)
		{
			throw new NullPointerException("Value in ResultValue is null");
		}
		else if (valueToConvert.value instanceof PickleArray)
		{
			throw new ResultValueConversionException("Can only convert primitive types");
		}

		switch (iTargetType)
		{
			case INTEGER:
				if (valueToConvert.iDatatype == SubClassif.BOOLEAN)
				{
					throw new ResultValueConversionException("Cannot convert Bool to Integer");
				}
				else if (valueToConvert.iDatatype == SubClassif.INTEGER)
				{
					returnVal = new ResultValue(valueToConvert.iDatatype
						, new Numeric(((Numeric) valueToConvert.value).stringValue, valueToConvert.iDatatype));
				}
				else if (valueToConvert.iDatatype == SubClassif.FLOAT)
				{
					returnVal = new ResultValue(SubClassif.INTEGER
						, "" + ((Numeric) valueToConvert.value).intValue);
				}
				else if (valueToConvert.iDatatype == SubClassif.STRING)
				{
					// Might be a better way to convert from a string, but it'll do for now
					returnVal= new ResultValue(SubClassif.INTEGER
						, "" + new Numeric("" + new Numeric(
							((StringBuilder) valueToConvert.value).toString()
								, SubClassif.FLOAT).intValue, SubClassif.INTEGER).intValue);
				}
				break;
			case FLOAT:
				if (valueToConvert.iDatatype == SubClassif.BOOLEAN)
				{
					throw new ResultValueConversionException("Cannot convert Bool to Float");
				}
				else if (valueToConvert.iDatatype == SubClassif.INTEGER)
				{
					returnVal = new ResultValue(SubClassif.FLOAT
						, "" + ((Numeric) valueToConvert.value).floatValue);
				}
				else if (valueToConvert.iDatatype == SubClassif.FLOAT)
				{
					returnVal = new ResultValue(valueToConvert.iDatatype
						, new Numeric(((Numeric) valueToConvert.value).stringValue
							, valueToConvert.iDatatype));
				}
				else if (valueToConvert.iDatatype == SubClassif.STRING)
				{
					returnVal = new ResultValue(SubClassif.FLOAT
						, "" + new Numeric("" + new Numeric(
							((StringBuilder) valueToConvert.value).toString()
								, SubClassif.FLOAT).floatValue, SubClassif.FLOAT).floatValue);
				}
				else
				{
					throw new ResultValueConversionException("Could not convert ResultValue to type Float");
				}
				break;
			case STRING:
				if (valueToConvert.iDatatype == SubClassif.STRING)
				{
					returnVal = new ResultValue(SubClassif.STRING
						, new StringBuilder(((StringBuilder) valueToConvert.value).toString()));
				}
				else if(valueToConvert.iDatatype == SubClassif.BOOLEAN)
				{
					if(((Boolean) valueToConvert.value).booleanValue())
					{
						returnVal = new ResultValue(SubClassif.STRING, new StringBuilder("T"));
					}
					else
					{
						returnVal = new ResultValue(SubClassif.STRING, new StringBuilder("F"));
					}
				}
				else if(valueToConvert.iDatatype == SubClassif.FLOAT ||
					valueToConvert.iDatatype == SubClassif.INTEGER)
				{
					returnVal = new ResultValue(SubClassif.STRING,
						new StringBuilder(((Numeric) valueToConvert.value).stringValue));
				}
				else
				{
					throw new ResultValueConversionException("could not convert the ResultValue to String");
				}
				break;
			case BOOLEAN:
				if(valueToConvert.iDatatype == SubClassif.BOOLEAN)
				{
					returnVal = new ResultValue(SubClassif.BOOLEAN
						, new Boolean (((Boolean) valueToConvert.value).booleanValue()));
				}
				else
				{
					throw new ResultValueConversionException("Cannot convert ResultValue to a Boolean value");
				}
				break;
			default:
				throw new ResultValueConversionException("Could not convert the ResultValue to type Boolean");
		}
		return returnVal;
	}

	/**
	 * Takes either a String or StringBuilder and converts to String.
	 * This is needed because some internals in ResultValue use StringBuilder, but some invocations of ResultValue in
	 *  parser use String.
	 * @param value An Object that is either a StringBuilder or String
	 * @return
	 */
	private String valueToString(Object value) {
		String strValue;
		if(value instanceof String)
			strValue = (String)value;
		else
			strValue = ((StringBuilder)value).toString();
		return strValue;
	}
	
	public boolean equals(ResultValue rv) {
		return (iDatatype == rv.iDatatype && value == rv.value);
	}

	@Override
	public String toString()
	{
		return value.toString();
	}
}