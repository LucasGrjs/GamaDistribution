# DistributionModel

Proof of concepts and experimentations for the article : "Distribution Model: Separation of Concerns To Facilitate the Distribution ofAgent-Based Models" for IJCAI 2025 [https://2025.ijcai.org/](https://2025.ijcai.org/)

# Requirements  
Java 17

mpirun (Open MPI) 4.1.4 (https://www.open-mpi.org/software/ompi/v4.1/)

Apache Maven 3.8.6

Java Binding for Open MPI (https://www.open-mpi.org/faq/?category=java)


# Compile the project

cd Gama_distribution/

cd gama

./travis/build.sh # compile the project (can take some time)

cd ../gama.experimental/gama.experimental.proxy/models/proxy/ # where to start the model to execute

# How to start examples : N > 2 and < 6*  (* the limit is the number of core on your machine)

./startMpiModel distributed_comodel/Distribution_model_KMEAN.xml N    # KMEAN model

./startMpiModel distributed_comodel/Distribution_model_GRID.xml 4     # GRID model

# Results 

All the results of these examples can be found in /output.log after the execution of above model.
/output.log/1/N/stdout will contains the logs of Processor N
/output.log/snapshot/N/ will contain the snapshot of the simualtion on Processor N
