/***
* Name: exportkml
* Author: Patrick Taillandier
* Description: 
* Tags: KML, KMZ, export, save
***/

model exportkml

global {
	file shapefile <- file("../includes/building.shp");
	geometry shape <- envelope(shapefile);
	date starting_date <- #now;
	
	//define the kml variable that will be used to store the geometry to display in the KML/KMZ file
	kml kml_export;
	
	geometry bounds;
	
	
	init {
		create building from: shapefile ;
		bounds <- union(building);
		create bug number: 5 with: [location:: any_location_in(bounds)];
	}
	reflex add_objects_to_kml {
		
		ask building {
			//add a geometry to the kml : add_geometry(kml, geometry, line width, border color, color)
			kml_export <- kml_export add_geometry (shape,2.0,#black,color);	
			
			//it is also possible to specify the begin date (current_date by default) and the ending date (current_date + step by default)
			//kml_export <- kml_export add_geometry (shape,2.0,#black,color, #now, #now plus_hours 1);	
		}
		ask bug {
			//add an icon to the kml: add_icon(kml,location,scale,orientation,file) ... like for add_geometry, it is also possible to specify the begin/end date
			kml_export <- kml_export add_icon (location,1.0,heading,"../includes/full_ant.png");
		}
	}
	
	reflex end_sim when: cycle = 5 {
		
		// export the kml to a kmz/kml file
		save kml_export to:"../results/result.kmz";
		do pause;
	}
}
species bug skills: [moving]{
	reflex move {
		do wander speed: 10.0 bounds: bounds;
	}
	aspect default {
		draw file("../includes/full_ant.png") size: 10 ;
	}
}


species building {
	rgb color <-rnd_color(155,255);
	reflex new_color {
		color <- rnd_color(155,255);
	}
	aspect default {
		draw shape color: color border: #black;
	}
}

experiment exportkml type: gui {
	output {
		display map {
			species building;
			species bug;
		}
	}
}
