library(tidyverse)
library(igraph)
library(edgebundleR)
#devtools::install_github("garthtarr/edgebundleR")

#nodes <- read_csv("nodes-csv")
links_diseases <- read_csv("edges-diseases.csv")
links_establishments <- read_csv("edges-establishments.csv")


links_diseases <- links_diseases %>%
    dplyr::rename("from"="country",
                  "to"="disease")  %>%
    drop_na

links_establishments <- links_establishments %>%
    dplyr::rename("from"="country",
                  "to"=`animal-category`) %>%
    drop_na


links=bind_rows(links_diseases,links_establishments) %>%
    mutate(to=str_replace_all(to," ","_"))

countries = unique(links$from)

nodes <- unique(c(links$from,links$to))
                
nodes <- data_frame(id=nodes) %>%
    mutate(label=id) %>%
    mutate(level=ifelse(label %in% countries,0,1  ))




 

net <- graph_from_data_frame(d=links, vertices=nodes, directed=F) 
V(net)$Type=ifelse(V(net)$label %in% countries,"blue", "black")
V(net)$color=V(net)$Type
    
edgebundle(net)

plot(net)



