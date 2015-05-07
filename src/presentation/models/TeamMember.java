/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package presentation.models;

/**
 *
 * @author atsampikakis
 */
public class TeamMember {
    
    private String name;
    private String imageUrl;
    private boolean hasBeenShot;
    
    
    
    public TeamMember(String name, String imageUrl, boolean hasBeenShot) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.hasBeenShot = hasBeenShot;
    }
    
    
    
    public void setTeamMemberHasBeenShot() {
        hasBeenShot = true;
    }
    
    
    
    public String getName() {
        return name;
    }
    
    
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    
    
    public boolean hasTeamMemberBeenShot() {
        return hasBeenShot;
    }
}
