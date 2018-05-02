package pers.philips;

import pers.philips.service.DeployService;

/**
 * App
 *
 */
public class App 
{

    public static void main( String[] args )
    {
        DeployService ds = new DeployService();
        ds.getAttachSolution();
    }



}
