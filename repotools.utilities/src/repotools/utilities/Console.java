package repotools.utilities;

// TODO: Auto-generated Javadoc
/**
 * The Class Console.
 */
public class Console
{
	public Console()
	{
		silent = false;
		detail = false;
	}
	
	public void write(String message)
	{
		if (silent == false)
		{
			System.out.print(message);
		}
	}
	
	public void writeLine(String message)
	{
		write(message + "\r\n");
	}
	
	public void writeLine()
	{
		writeLine("");
	}

	public void reportException(Exception ex)
	{
		writeLine(ex.getClass().toString() + ": " + ex.getMessage());
		writeLine(ex.getStackTrace().toString());
	}

	public boolean checkConfirm()
	{
		String confirmString = System.console().readLine();
		
		if (confirmString.startsWith("y") ||
			confirmString.startsWith("Y"))
		{
			return true;
		}
		
		return false;
	}

	public boolean isSilent()
	{
		return silent;
	}

	public void setSilent(boolean silent)
	{
		this.silent = silent;
	}

	public boolean isDetail()
	{
		return detail;
	}

	public void setDetail(boolean detail)
	{
		this.detail = detail;
	}

	/** The silent. */
	protected boolean silent;
	
	/** The detail. */
	protected boolean detail;
}
