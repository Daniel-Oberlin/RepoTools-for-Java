package repotools.utilities;

import java.util.ArrayList;
import java.util.HashMap;


public class ArgUtilities
{
	public ArgUtilities(String[] argsArg, Console consoleArg)
	{
		args = argsArg;
		console = consoleArg;
		argToPositionsMap = new HashMap<String, ArrayList<Integer>>();
		argWasChecked = new boolean[args.length];
		
		for (int i = 0; i < argsArg.length; i++)
		{
			String nextArg = args[i];
			
			if (argToPositionsMap.get(nextArg) == null)
			{
				argToPositionsMap.put(nextArg, new ArrayList<Integer>());
			}
			
			argToPositionsMap.get(nextArg).add(i);
			argWasChecked[i] = false;
		}
	}
	
	public int getLength()
	{
		return args.length;
	}
	
	public String getArg(int i)
	{
		argWasChecked[i] = true;
		return args[i];
	}
	
	public boolean hasOption(String optionName)
	{
		String argName = makeArgFromOptionName(optionName);
		
		if (argToPositionsMap.keySet().contains(argName) == true)
		{
			for (int i : argToPositionsMap.get(argName))
			{
				argWasChecked[i] = true;
			}
			
			return true;
		}
		
		return false;
	}
	
	public ArrayList<String> getParametersForOption(String optionName)
	{	
		ArrayList<String> parameters = new ArrayList<String>();
		
		if (hasOption(optionName) == false)
		{
			return parameters;
		}
		
		String argName = makeArgFromOptionName(optionName);
		ArrayList<Integer> optionPositions = argToPositionsMap.get(argName);
		
		for (int optionPosition : optionPositions)
		{
			int parameterPosition = optionPosition + 1;
			
			if (args.length > parameterPosition)
			{		
				argWasChecked[parameterPosition] = true;
				parameters.add(args[parameterPosition]);
			}
			else
			{
				console.writeLine("No parameter found for option \"" + argName + "\"");
				System.exit(1);
			}
		}
		
		return parameters;
	}
	
	public String getParameterForOption(String optionName)
	{
		if (hasOption(optionName) == false)
		{
			return null;
		}
		
		ArrayList<String> parameters = getParametersForOption(optionName);
		return parameters.get(parameters.size() - 1);
	}
	
	public ArrayList<String> getUncheckedArgs()
	{
		ArrayList<String> uncheckedArgs = new ArrayList<String>();
		
		for (int i = 0; i < argWasChecked.length; i++)
		{
			if (argWasChecked[i] == false)
			{
				uncheckedArgs.add(args[i]);
			}
		}
		
		return uncheckedArgs;
	}
	
	public void exitWithErrorWhenUncheckedArgs()
	{
		ArrayList<String> uncheckedArgs = getUncheckedArgs();
		
		if (uncheckedArgs.size() > 0)
		{
			console.writeLine("Unrecognized parameter \" " + uncheckedArgs.get(0) + "\"");
			System.exit(1);
		}
	}
	
	private String makeArgFromOptionName(String optionName)
	{
		return "-" + optionName;
	}
	
	private String[] args;
	private HashMap<String, ArrayList<Integer>> argToPositionsMap;
	private boolean[] argWasChecked;
	private Console console;
}
