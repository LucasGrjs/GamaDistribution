/**
* Name: Testconnection
* Author: hqnghi
* Description: 
* Tags: Tag1, Tag2, TagN
*/
model Testconnection

global {
	file shp_file <- shape_file("../includes/commune_myxuyen.shp");
	//	file netcdf_sample <- file("../includes/ENS_mm_rcp45.2015_2050_MKD_pr.nc");
	//	file netcdf_sample <- file("../includes/ENS_mm_rcp45.2015_2050_MKD_tas.nc");
	//	file netcdf_sample <- file("../includes/ENS_mm_rcp85.2015_2050_MKD_pr.nc");
	file netcdf_sample <- file("../includes/ENS_mm_rcp85.2015_2050_MKD_tas.nc");
	//	file netcdf_sample <- file("../includes/tos_O1_2001-2002.nc");
	geometry shape <- to_GAMA_CRS(envelope(netcdf_sample), "4326");
	//	geometry shape <- envelope(netcdf_sample);
	//	geometry shape <- envelope(shp_file);
	int times <- 1;
	int grid_num <- 0;
	int gridsSize <- 0;
	int timesAxisSize <- 0;
	field field_from_matrix;

	init {
	//		write openDataSet(netcdf_sample);
		gridsSize <- getGridsSize(netcdf_sample);
		timesAxisSize <- netcdf_sample getTimeAxisSize grid_num;
		create shp from: shp_file;
		matrix<int> m <- (matrix<int>(netcdf_sample readDataSlice (grid_num, times, 0, -1, -1)));
		field_from_matrix <- field(m);
	}

	reflex s {
		matrix<int> m <- (matrix<int>(netcdf_sample readDataSlice (grid_num, times, 0, -1, -1)));
		field_from_matrix <- field(m);
		//		ask cell {
		//			grid_value <- float(m at {grid_x, grid_y});
		//			color <- rgb(grid_value);
		//		}
		times <- times + 1;
		if (times > timesAxisSize - 1) {
			times <- 0;
		}

		grid_num <- grid_num + 1;
		if (grid_num > gridsSize - 1) {
			grid_num <- 0;
			timesAxisSize <- netcdf_sample getTimeAxisSize grid_num;
		}

	}

}

species shp {
}
//grid cell file: netcdf_sample {
//
//	init {
//		color <- rgb(grid_value);
//	}
//
//}
experiment sim type: gui {
	list<rgb> palette <- brewer_colors(any(brewer_palettes(0)));
	output {
		display "s" type: opengl { //camera_pos: {316512.7586,169804.7114,32663.0105} camera_look_pos: {316512.7586,169804.1413,-0.0675} camera_up_vector: {0.0,1.0,0.0}{ 
		//			grid cell;
			mesh field_from_matrix color: palette triangulation: true smooth: 4;
			species shp;
		}

	}

}
