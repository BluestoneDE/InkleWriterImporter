package inkle.json;

import java.util.ArrayList;

public class Option {
    public String option = "new option";
    public String linkPath;

    public ArrayList<Condition> ifConditions, notIfConditions;

    public Option() {
    }

    public Option(String option) {
        setOption(option);
    }

    public Option(String option, String linkPath) {
        this(option);
        setLinkPath(linkPath);
    }

    public Option(String option, String linkPath, ArrayList<Condition> ifConditions, ArrayList<Condition> notIfConditions) {
        this(option, linkPath);
        setIfConditions(ifConditions);
        setNotIfConditions(notIfConditions);
    }

    public String getOption() {
        return option;
    }

    public void setOption(String option) {
        this.option = option;
    }

    public String getLinkPath() {
        return linkPath;
    }

    public boolean hasLinkPath() {
        return linkPath != null && linkPath.length() > 0;
    }

    public void setLinkPath(String linkPath) {
        if (linkPath.length() > Stitch.keyLength && !Character.isDigit(linkPath.charAt(16)))
            linkPath = linkPath.substring(0, Stitch.keyLength);
        this.linkPath = linkPath;
    }

    public ArrayList<Condition> getIfConditions() {
        return ifConditions;
    }

    public void setIfConditions(ArrayList<Condition> ifConditions) {
        this.ifConditions = ifConditions;
    }

    public void addIfCondition(Condition ifCondition) {
        ifConditions.add(ifCondition);
    }

    public ArrayList<Condition> getNotIfConditions() {
        return notIfConditions;
    }

    public void setNotIfConditions(ArrayList<Condition> notIfConditions) {
        this.notIfConditions = notIfConditions;
    }
    
    public void addNotIfCondition(Condition notIfCondition) {
        notIfConditions.add(notIfCondition);
    }
}
